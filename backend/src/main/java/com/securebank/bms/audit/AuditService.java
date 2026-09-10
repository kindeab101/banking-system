package com.securebank.bms.audit;

import com.securebank.bms.entity.AuditLog;
import com.securebank.bms.entity.AuditResult;
import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UserAccount actor, String action, String entityType, String entityReference,
                       AuditResult result, String metadata) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityReference(entityReference);
        log.setResult(result);
        log.setMetadata(metadata);
        HttpServletRequest request = currentRequest();
        if (request != null) {
            log.setIpAddress(clientIp(request));
            String ua = request.getHeader("User-Agent");
            if (ua != null && ua.length() > 250) {
                ua = ua.substring(0, 250);
            }
            log.setUserAgent(ua);
        }
        repository.save(log);
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
