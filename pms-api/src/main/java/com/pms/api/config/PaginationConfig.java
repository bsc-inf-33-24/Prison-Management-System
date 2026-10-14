package com.pms.api.config;

import com.pms.api.common.pagination.PaginationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaginationProperties.class)
public class PaginationConfig {
}
