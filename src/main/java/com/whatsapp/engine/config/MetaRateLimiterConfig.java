package com.whatsapp.engine.config;

import org.springframework.context.annotation.Bean;
import com.google.common.util.concurrent.RateLimiter;

public class MetaRateLimiterConfig {

    @Bean
    public RateLimiter whatsappRateLimiter() {
        return RateLimiter.create(3.0); // 3 requests per second
    }
}
