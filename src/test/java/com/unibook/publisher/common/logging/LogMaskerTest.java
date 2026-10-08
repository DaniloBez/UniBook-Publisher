package com.unibook.publisher.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogMaskerTest {

    @Test
    @DisplayName("Маскує пароль у форматі key=value")
    void masksPasswordKeyValue() {
        assertEquals("login user=a password=****", LogMasker.mask("login user=a password=secret123"));
    }

    @Test
    @DisplayName("Маскує пароль і токен у JSON")
    void masksJsonFields() {
        String masked = LogMasker.mask("{\"email\":\"a@b.c\",\"password\":\"p@ss w0rd\",\"token\":\"abc.def\"}");

        assertEquals("{\"email\":\"a@b.c\",\"password\":****,\"token\":****}", masked);
    }

    @Test
    @DisplayName("Маскує Bearer-токен")
    void masksAuthorization() {
        assertEquals("Authorization: ****", LogMasker.mask("Authorization: Bearer eyJhbGciOi.abc"));
    }

    @Test
    @DisplayName("Маскує номер картки (Luhn), залишаючи останні 4 цифри")
    void masksCardNumber() {
        assertEquals("card ************1111", LogMasker.mask("card 4111 1111 1111 1111"));
    }

    @Test
    @DisplayName("Не чіпає довгі числа, що не є картками, і UUID")
    void keepsNonCardNumbers() {
        String text = "id 1234567890123 uuid 550e8400-e29b-41d4-a716-446655440000";
        assertEquals(text, LogMasker.mask(text));
    }

    @Test
    @DisplayName("Маскує IBAN")
    void masksIban() {
        assertEquals("to ****", LogMasker.mask("to UA213223130000026007233566001"));
    }

    @Test
    @DisplayName("null лишається null")
    void nullSafe() {
        assertNull(LogMasker.mask(null));
    }
}
