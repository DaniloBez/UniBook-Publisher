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

    public DiffResponse compare(UUID fromRevisionId, UUID toRevisionId, String oldText, String newText) {
        List<String> oldLines = oldText != null ? List.of(oldText.split("\\r?\\n")) : List.of();
        List<String> newLines = newText != null ? List.of(newText.split("\\r?\\n")) : List.of();

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        List<DiffLine> lines = new ArrayList<>();

        int oldIndex = 0;
        int newIndex = 0;

        for(var delta : patch.getDeltas()) {
            while (oldIndex < delta.getSource().getPosition()) {
                lines.add(new DiffLine(oldLines.get(oldIndex), DiffStatus.EQUAL, oldIndex + 1, newIndex + 1));
                oldIndex++;
                newIndex++;
            }

            switch (delta.getType()) {
                case DELETE -> {
                    for(String line : delta.getSource().getLines()) {
                        lines.add(new DiffLine(line, DiffStatus.DELETED, oldIndex + 1, null));
                        oldIndex++;
                    }
                }
                case INSERT -> {
                    for(String line : delta.getTarget().getLines()) {
                        lines.add(new DiffLine(line, DiffStatus.INSERTED, null, newIndex + 1));
                        newIndex++;
                    }
                }
                case CHANGE -> {
                    for(String line : delta.getSource().getLines()) {
                        lines.add(new DiffLine(line, DiffStatus.DELETED, oldIndex + 1, null));
                        oldIndex++;
                    }
                    for(String line : delta.getTarget().getLines()) {
                        lines.add(new DiffLine(line, DiffStatus.INSERTED, null, newIndex + 1));
                        newIndex++;
                    }
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
}
