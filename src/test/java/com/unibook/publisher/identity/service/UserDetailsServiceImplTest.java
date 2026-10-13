package com.unibook.publisher.identity.service;

import com.unibook.publisher.identity.repository.UserRepository;
import com.unibook.publisher.identity.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("Повертає User, коли користувача знайдено за email")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        String email = "author@unibook.com";
        User user = mock(User.class);

        when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername(email);

        assertNotNull(result);
        assertEquals(user, result);
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
