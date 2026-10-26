package com.pms.api.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.pms.api.common.audit.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return result;
        }

        String username = authentication.getName();
        String action = joinPoint.getSignature().getName().toUpperCase(Locale.ROOT);
        String entityName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String publicRecordId = extractRecordId(result);

        auditService.record(username, action, entityName, publicRecordId,
                serialize(joinPoint.getArgs()), serialize(result));
        return result;
    }

    private String extractRecordId(Object value) {
        if (value == null) {
            return null;
        }

        for (String methodName : new String[] {"getPublicUuid", "getUuid", "getId"}) {
            try {
                Method method = value.getClass().getMethod(methodName);
                Object resolved = method.invoke(value);
                if (resolved != null) {
                    return resolved.toString();
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next common identifier accessor.
            }
        }

        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        return value.getClass().getSimpleName();
    }

    private String serialize(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            LOGGER.debug("Unable to serialize audit payload for {}", value.getClass().getSimpleName(), exception);
            return value.getClass().getSimpleName();
        }
    }
}
