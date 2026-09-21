package com.unibook.publisher.common.logging;

import java.time.Instant;

public class SystemOutAppLogger implements AppLogger {

    @Override
    public void info(String message, Object... args) {
        log("INFO", message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        log("WARN", message, args);
    }

    @Override
    public void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    private void log(String level, String message, Object... args) {
        System.out.println(
                Instant.now()
                        + " [" + level + "] "
                        + format(message, args)
        );
    }

    private String format(String message, Object... args) {
        String result = message;

        for (Object arg : args) {
            result = result.replaceFirst(
                    "\\{\\}",
                    java.util.regex.Matcher.quoteReplacement(String.valueOf(arg))
            );
        }

        return result;
    }
}
