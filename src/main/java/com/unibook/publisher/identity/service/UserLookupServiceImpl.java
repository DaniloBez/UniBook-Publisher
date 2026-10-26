package com.unibook.publisher.identity.service;

import com.unibook.publisher.identity.api.UserLookupService;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserLookupServiceImpl implements UserLookupService {
    private final UserRepository userRepository;

    public UserLookupServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSummary> findById(UUID id) {
        if (id == null) return Optional.empty();
        return userRepository.findByIdWithProfile(id).map(UserSummary::from);
    }

    @Override
    public Map<UUID, UserSummary> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllByIdWithProfile(ids).stream()
                .map(UserSummary::from)
                .collect(Collectors.toMap(UserSummary::id, Function.identity(), (us1, us2) -> us1));
    }
}
