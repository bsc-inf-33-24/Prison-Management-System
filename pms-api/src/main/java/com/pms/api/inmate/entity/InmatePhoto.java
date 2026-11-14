package com.pms.api.inmate.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inmate_photos")
public class InmatePhoto extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false, unique = true)
    private Inmate inmate;

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    protected InmatePhoto() { }

    public InmatePhoto(Inmate inmate, String objectKey, String contentType, long fileSize, User uploadedBy) {
        this.inmate = inmate;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public String getObjectKey() { return objectKey; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
}
