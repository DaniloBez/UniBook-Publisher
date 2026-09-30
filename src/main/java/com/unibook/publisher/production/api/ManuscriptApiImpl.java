package com.unibook.publisher.production.api;

import com.unibook.publisher.production.repository.ManuscriptRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class ManuscriptApiImpl implements ManuscriptApi {
    private final ManuscriptRepository repository;

    ManuscriptApiImpl(ManuscriptRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ManuscriptDto> findById(UUID manuscriptId) {
        return repository.findById(manuscriptId).map(m -> new ManuscriptDto(m.getManuscriptId(), m.getTitle()));
    }
}