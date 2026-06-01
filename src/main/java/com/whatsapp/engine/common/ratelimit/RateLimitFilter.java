package com.whatsapp.engine.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.common.api.ApiResponse;
import com.whatsapp.engine.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;

    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper, RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPaths().stream().anyMatch(path::contains);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = clientKey(request);
        RateLimitBucket bucket = buckets.compute(key, (ignored, existing) -> nextBucket(existing));
        int count = bucket.counter().incrementAndGet();

        response.setHeader("X-RateLimit-Limit", String.valueOf(properties.getRequestsPerMinute()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, properties.getRequestsPerMinute() - count)));

        if (count > properties.getRequestsPerMinute()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.failure(
                    "Rate limit exceeded",
                    new ApiError("RATE_LIMIT_EXCEEDED", "Too many requests", null)
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitBucket nextBucket(RateLimitBucket existing) {
        long now = Instant.now().getEpochSecond();
        if (existing == null || now - existing.windowStartedAtEpochSecond() >= WINDOW_SECONDS) {
            return new RateLimitBucket(now, new AtomicInteger(0));
        }
        return existing;
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitBucket(long windowStartedAtEpochSecond, AtomicInteger counter) {
    }
}
