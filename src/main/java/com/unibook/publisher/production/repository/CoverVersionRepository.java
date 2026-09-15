package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.CoverVersion;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CoverVersionRepository {
    private final ConcurrentHashMap<UUID, CoverVersion> coverVersions = new ConcurrentHashMap<>();

    public CoverVersion save(CoverVersion coverVersion) {
        coverVersions.put(coverVersion.id(), coverVersion);
        return coverVersion;
    }

    public Optional<CoverVersion> findById(UUID id) {
        return Optional.ofNullable(coverVersions.get(id));
    }

    public List<CoverVersion> findByManuscriptId(UUID manuscriptId) {
        return coverVersions.values().stream()
                .filter(coverVersion -> coverVersion.manuscriptId().equals(manuscriptId))
                .sorted(Comparator.comparingInt(CoverVersion::versionNumber))
                .toList();
    }

    public Optional<CoverVersion> findLatestByManuscriptId(UUID manuscriptId) {
        return coverVersions.values().stream()
                .filter(coverVersion -> coverVersion.manuscriptId().equals(manuscriptId))
                .max(Comparator.comparingInt(CoverVersion::versionNumber));
    }
}
