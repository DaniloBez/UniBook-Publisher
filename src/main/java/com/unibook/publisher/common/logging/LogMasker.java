package com.unibook.publisher.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogMasker {

    public static final String MASK = "****";

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)((?:password|passwd|pwd|secret|token|access[-_]?key|secret[-_]?key|authorization|card[-_]?number|cvv|cvc|iban)"
                    + "[\"']?\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|(?:Bearer\\s+)?[^\\s,;&\"'}\\]]+)");

    private static final Pattern CARD_NUMBER = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{11,30}\\b");

    private LogMasker() {
    }

    public static String mask(String message) {
        if (message == null || message.isEmpty())
            return message;

        String result = KEY_VALUE.matcher(message).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + MASK));
        result = CARD_NUMBER.matcher(result).replaceAll(m -> maskCard(m.group()));
        return IBAN.matcher(result).replaceAll(MASK);
    }

    private static String maskCard(String candidate) {
        String digits = candidate.replaceAll("[ -]", "");
        if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits))
            return candidate;

        return "************" + digits.substring(digits.length() - 4);
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
