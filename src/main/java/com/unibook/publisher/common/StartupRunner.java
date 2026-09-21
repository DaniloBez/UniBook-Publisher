package com.unibook.publisher.common;

import com.unibook.publisher.common.logging.AppLogger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final AppLogger logger;

    public StartupRunner(AppLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run(String... args) { //we will put here anything for dev runtime purposes, e.g. populate h2 db with demo data
        logger.info("Startup logger test: {}", "Hello from Spring");
    }
}
