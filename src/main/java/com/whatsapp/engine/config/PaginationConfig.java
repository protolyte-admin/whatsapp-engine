package com.whatsapp.engine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer(
            @Value("${app.pagination.default-page-size:20}") int defaultPageSize,
            @Value("${app.pagination.max-page-size:100}") int maxPageSize,
            @Value("${app.pagination.one-indexed-parameters:false}") boolean oneIndexedParameters
    ) {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, defaultPageSize));
            resolver.setMaxPageSize(maxPageSize);
            resolver.setOneIndexedParameters(oneIndexedParameters);
        };
    }
}
