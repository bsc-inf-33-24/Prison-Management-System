package com.pms.api.inmate.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.entity.InmatePhoto;
import com.pms.api.inmate.repository.InmatePhotoRepository;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InmatePhotoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InmatePhotoService.class);
    private static final int MAX_DIMENSION = 6000;
    private static final long MAX_PIXELS = 20_000_000L;
    private final MinioClient minio;
    private final InmatePhotoRepository photos;
    private final InmateRepository inmates;
    private final UserRepository users;
    private final PrisonAccessService prisonAccess;
    private final String bucket;
    private final long maxBytes;

    public InmatePhotoService(MinioClient minio, InmatePhotoRepository photos, InmateRepository inmates,
            UserRepository users, PrisonAccessService prisonAccess,
            @Value("${app.storage.minio.bucket}") String bucket,
            @Value("${app.storage.minio.max-photo-bytes}") long maxBytes) {
        this.minio = minio;
        this.photos = photos;
        this.inmates = inmates;
        this.users = users;
        this.prisonAccess = prisonAccess;
        this.bucket = bucket;
        this.maxBytes = maxBytes;
    }

    @Transactional
    public void store(Inmate inmate, MultipartFile upload, String username) {
        if (upload == null || upload.isEmpty()) return;
        SanitizedImage image = sanitize(upload);
        User uploader = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        String extension = "image/jpeg".equals(image.contentType()) ? "jpg" : "png";
        String objectKey = "inmates/" + inmate.getPublicUuid() + "/" + UUID.randomUUID() + "." + extension;
        try {
            ensureBucket();
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .stream(new ByteArrayInputStream(image.bytes()), image.bytes().length, -1)
                    .contentType(image.contentType()).build());
            photos.saveAndFlush(new InmatePhoto(inmate, objectKey, image.contentType(), image.bytes().length, uploader));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) removeObjectQuietly(objectKey);
                    }
                });
            }
        } catch (RuntimeException exception) {
            removeObjectQuietly(objectKey);
            throw exception;
        } catch (Exception exception) {
            removeObjectQuietly(objectKey);
            throw new PhotoStorageException("The inmate photo could not be stored.", exception);
        }
    }

    @Transactional(readOnly = true)
    public PhotoContent get(UUID inmatePublicId) {
        Inmate inmate = inmates.findByPublicUuid(inmatePublicId).orElseThrow(InmateNotFoundException::new);
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        InmatePhoto photo = photos.findByInmateId(inmate.getId()).orElseThrow(PhotoNotFoundException::new);
        try (InputStream input = minio.getObject(GetObjectArgs.builder().bucket(bucket)
                .object(photo.getObjectKey()).build())) {
            return new PhotoContent(input.readAllBytes(), photo.getContentType());
        } catch (Exception exception) {
            throw new PhotoStorageException("The inmate photo could not be retrieved.", exception);
        }
    }

    private SanitizedImage sanitize(MultipartFile upload) {
        try {
            if (upload.getSize() > maxBytes) throw new IllegalArgumentException("Photo must not exceed 5 MB.");
            byte[] source = upload.getBytes();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
                if (input == null) throw new IllegalArgumentException("Photo must be a valid JPEG or PNG image.");
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Photo must be a valid JPEG or PNG image.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (!(format.equals("jpeg") || format.equals("jpg") || format.equals("png")))
                        throw new IllegalArgumentException("Only JPEG and PNG photos are supported.");
                    if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                            || (long) width * height > MAX_PIXELS)
                        throw new IllegalArgumentException("Photo dimensions are too large.");
                    java.awt.image.BufferedImage decoded = reader.read(0);
                    if (decoded == null) throw new IllegalArgumentException("Photo is not a decodable image.");
                    String outputFormat = format.equals("png") ? "png" : "jpeg";
                    java.awt.image.BufferedImage normalized = decoded;
                    if (outputFormat.equals("jpeg") && decoded.getType() != java.awt.image.BufferedImage.TYPE_INT_RGB) {
                        normalized = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        java.awt.Graphics2D graphics = normalized.createGraphics();
                        graphics.drawImage(decoded, 0, 0, java.awt.Color.WHITE, null);
                        graphics.dispose();
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(normalized, outputFormat, output))
                        throw new IllegalArgumentException("Photo format is not supported.");
                    byte[] sanitized = output.toByteArray();
                    if (sanitized.length > maxBytes) throw new IllegalArgumentException("Processed photo must not exceed 5 MB.");
                    return new SanitizedImage(sanitized, outputFormat.equals("png") ? "image/png" : "image/jpeg");
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Photo could not be read as a valid image.", exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    private void removeObjectQuietly(String objectKey) {
        try { minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build()); }
        catch (Exception exception) { LOGGER.warn("Could not clean up an uncommitted inmate photo object."); }
    }

    private record SanitizedImage(byte[] bytes, String contentType) { }
    public record PhotoContent(byte[] bytes, String contentType) { }
    public static class InmateNotFoundException extends RuntimeException { }
    public static class PhotoNotFoundException extends RuntimeException { }
    public static class PhotoStorageException extends RuntimeException {
        public PhotoStorageException(String message, Throwable cause) { super(message, cause); }
    }
}
