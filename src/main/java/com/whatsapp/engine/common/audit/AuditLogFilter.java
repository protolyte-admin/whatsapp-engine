package com.whatsapp.engine.common.audit;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.audit.repository.AuditLogRepository;
import com.whatsapp.engine.common.logging.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogRepository auditLogRepository;
    private final boolean auditEnabled;

    public AuditLogFilter(
            AuditLogRepository auditLogRepository,
            @Value("${app.audit.enabled:true}") boolean auditEnabled
    ) {
        this.auditLogRepository = auditLogRepository;
        this.auditEnabled = auditEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !auditEnabled
                || path.contains("/actuator/")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.endsWith("/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            writeAuditLog(request, response, System.currentTimeMillis() - startedAt);
        }
    }

    private void writeAuditLog(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setCorrelationId((String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
            auditLog.setMethod(request.getMethod());
            auditLog.setPath(request.getRequestURI());
            auditLog.setAction(request.getMethod() + " " + request.getRequestURI());
            auditLog.setClientIp(clientIp(request));
            auditLog.setUserAgent(truncate(request.getHeader("User-Agent"), 300));
            auditLog.setStatusCode(response.getStatus());
            auditLog.setDurationMs(durationMs);
            auditLog.setOccurredAt(Instant.now());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                auditLog.setUserId(user.getId());
                auditLog.setUsername(user.getEmail());
                auditLog.setOrganizationId(user.getOrganization().getId());
            }

            auditLogRepository.save(auditLog);
            log.info(
                    "http_request method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to write audit log: {}", exception.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
