package com.unibook.publisher.production.service;

import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.enums.DiffStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DiffServiceTest {

    private DiffService diffService;

    @BeforeEach
    void setUp() {
        diffService = new DiffService();
    }

    @Test
    void compare_Equal() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        List<String> oldLines = List.of("Перший рядок", "Другий рядок");
        List<String> newLines = List.of("Перший рядок", "Другий рядок");

        DiffResponse response = diffService.compare(fromId, toId, oldLines, newLines);

        assertNotNull(response);
        assertEquals(fromId, response.fromRevisionId());
        assertEquals(toId, response.toRevisionId());
        assertEquals(2, response.lines().size());

        assertEquals(DiffStatus.EQUAL, response.lines().getFirst().status());
        assertEquals("Перший рядок", response.lines().getFirst().content());
        assertEquals(1, response.lines().get(0).oldLineIndex());
        assertEquals(1, response.lines().get(0).newLineIndex());

        assertEquals(DiffStatus.EQUAL, response.lines().get(1).status());
        assertEquals("Другий рядок", response.lines().get(1).content());
    }

    @Test
    void compare_Inserted() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        List<String> oldLines = List.of("Перший рядок");
        List<String> newLines = List.of("Перший рядок", "Другий рядок");

        DiffResponse response = diffService.compare(fromId, toId, oldLines, newLines);

        assertEquals(2, response.lines().size());
        assertEquals(DiffStatus.EQUAL, response.lines().get(0).status());
        assertEquals(DiffStatus.INSERTED, response.lines().get(1).status());
        assertEquals("Другий рядок", response.lines().get(1).content());
        assertNull(response.lines().get(1).oldLineIndex());
        assertEquals(2, response.lines().get(1).newLineIndex());
    }

    @Test
    void compare_Deleted() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        List<String> oldLines = List.of("Перший рядок", "Другий рядок");
        List<String> newLines = List.of("Перший рядок");

        DiffResponse response = diffService.compare(fromId, toId, oldLines, newLines);

        assertEquals(2, response.lines().size());
        assertEquals(DiffStatus.EQUAL, response.lines().get(0).status());
        assertEquals(DiffStatus.DELETED, response.lines().get(1).status());
        assertEquals("Другий рядок", response.lines().get(1).content());
        assertEquals(2, response.lines().get(1).oldLineIndex());
        assertNull(response.lines().get(1).newLineIndex());
    }

    @Test
    void compare_Changed() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        List<String> oldLines = List.of("Старий текст");
        List<String> newLines = List.of("Новий текст");

        DiffResponse response = diffService.compare(fromId, toId, oldLines, newLines);

        assertEquals(2, response.lines().size());
        assertEquals(DiffStatus.DELETED, response.lines().get(0).status());
        assertEquals("Старий текст", response.lines().get(0).content());
        assertEquals(DiffStatus.INSERTED, response.lines().get(1).status());
        assertEquals("Новий текст", response.lines().get(1).content());
    }

    @Test
    void compare_EmptyLists() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        DiffResponse response = diffService.compare(fromId, toId, List.of(), List.of());

        assertNotNull(response);
        assertTrue(response.lines().isEmpty());
    }
}
