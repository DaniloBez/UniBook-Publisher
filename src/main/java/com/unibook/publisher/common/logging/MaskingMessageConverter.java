package com.unibook.publisher.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return LogMasker.getMask(event.getFormattedMessage());
    }
}
