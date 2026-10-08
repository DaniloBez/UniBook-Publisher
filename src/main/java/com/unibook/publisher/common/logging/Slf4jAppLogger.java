package com.unibook.publisher.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jAppLogger implements AppLogger {

    private static final Logger log = LoggerFactory.getLogger("com.unibook.publisher.app");

    @Override
    public void info(String message, Object... args) {
        log.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        log.warn(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        log.error(message, args);
    }
}
