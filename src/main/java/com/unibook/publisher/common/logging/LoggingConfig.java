package com.unibook.publisher.common.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public AppLogger appLogger() {
        return new SystemOutAppLogger();
    }
}
