package com.unibook.publisher.common.logging;

public interface AppLogger {

    void info(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);
}
