package com.unibook.publisher.production.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.unibook.publisher.production.entity.DiffLine;
import com.unibook.publisher.production.entity.response.DiffResponse;
import com.unibook.publisher.production.enums.DiffStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DiffService {

    public DiffResponse compare(UUID fromRevisionId, UUID toRevisionId, List<String> oldLines, List<String> newLines) {
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        List<DiffLine> lines = new ArrayList<>();

        int oldIndex = 0;
        int newIndex = 0;

        for (var delta : patch.getDeltas()) {
            while (oldIndex < delta.getSource().getPosition()) {
                lines.add(new DiffLine(oldLines.get(oldIndex), DiffStatus.EQUAL, oldIndex + 1, newIndex + 1));
                oldIndex++;
                newIndex++;
            }

            switch (delta.getType()) {
                case DELETE -> oldIndex = appendDeletedLines(lines, delta.getSource().getLines(), oldIndex);
                case INSERT -> newIndex = appendInsertedLines(lines, delta.getTarget().getLines(), newIndex);
                case CHANGE -> {
                    oldIndex = appendDeletedLines(lines, delta.getSource().getLines(), oldIndex);
                    newIndex = appendInsertedLines(lines, delta.getTarget().getLines(), newIndex);
                }
            }
        }

        while (oldIndex < oldLines.size()) {
            lines.add(new DiffLine(oldLines.get(oldIndex), DiffStatus.EQUAL, oldIndex + 1, newIndex + 1));
            oldIndex++;
            newIndex++;
        }

        return new DiffResponse(fromRevisionId, toRevisionId, lines);
    }

    private int appendDeletedLines(List<DiffLine> lines, List<String> sourceLines, int oldIndex) {
        for (String line : sourceLines) {
            lines.add(new DiffLine(line, DiffStatus.DELETED, oldIndex + 1, null));
            oldIndex++;
        }
        return oldIndex;
    }

    private int appendInsertedLines(List<DiffLine> lines, List<String> targetLines, int newIndex) {
        for (String line : targetLines) {
            lines.add(new DiffLine(line, DiffStatus.INSERTED, null, newIndex + 1));
            newIndex++;
        }
        return newIndex;
    }
}