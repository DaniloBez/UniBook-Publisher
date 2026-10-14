package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.identity.repository.UserRepository;
import com.unibook.publisher.identity.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl jpaUserDetailsService;

    @Test
    @DisplayName("Повертає AuthenticatedUser, коли користувача знайдено за email")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        String email = "author@unibook.com";
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setHashedPassword("hash");
        user.setRole(UserRole.AUTHOR);

        when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername(email);

        assertNotNull(result);
        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class, result);
        assertEquals(id, principal.id());
        assertEquals(email, principal.getUsername());
        assertEquals("hash", principal.getPassword());
        assertEquals(List.of("ROLE_AUTHOR"), principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        verify(userRepository).getByEmail(email);
    }

    @Test
    @DisplayName("Викидає UsernameNotFoundException, коли користувача з таким email не існує")
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        String email = "nonexistent@unibook.com";

        when(userRepository.getByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> jpaUserDetailsService.loadUserByUsername(email)
        );

        assertEquals("Користувача з email nonexistent@unibook.com не знайдено.", exception.getMessage());
        verify(userRepository).getByEmail(email);
    }
}
