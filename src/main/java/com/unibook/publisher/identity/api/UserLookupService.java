package com.unibook.publisher.identity.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserLookupService {
    Optional<UserSummary> findById(UUID id);
    Map<UUID, UserSummary> findAllByIds(Collection<UUID> ids);
}
