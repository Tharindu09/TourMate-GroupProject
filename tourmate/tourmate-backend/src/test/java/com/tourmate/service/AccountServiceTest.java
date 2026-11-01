package com.tourmate.service;

import com.tourmate.dto.*;
import com.tourmate.model.User;
import com.tourmate.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    @Test void registrationNormalizesEmailAndHashesPasswordBeforeSaving() {
        var users = mock(UserRepository.class);
        var passwords = mock(PasswordEncoder.class);
        var encoder = mock(JwtEncoder.class);
        when(users.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwords.encode("correct-password")).thenReturn("hashed-password");
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {User user = invocation.getArgument(0); user.id = 7L; return user;});
        when(encoder.encode(any())).thenReturn(new Jwt("signed-token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "HS256"), Map.of("sub", "7")));
        var result = new AuthService(users, passwords, encoder).register(new RegisterRequest(" Test ", " Traveller ", " Test@Example.COM ", "correct-password"));
        assertEquals("test@example.com", result.user().email());
        assertEquals("Test", result.user().firstName());
        assertEquals("signed-token", result.accessToken());
        verify(users).saveAndFlush(argThat(user -> "hashed-password".equals(user.passwordHash) && "USER".equals(user.role)));
    }
    @Test void loginRejectsWrongPasswordsAndOversizedUtf8Passwords() {
        var users = mock(UserRepository.class);
        var passwords = mock(PasswordEncoder.class);
        var encoder = mock(JwtEncoder.class);
        User user = new User(); user.passwordHash = "hash";
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("wrong", "hash")).thenReturn(false);
        var auth = new AuthService(users, passwords, encoder);
        assertEquals(401, assertThrows(ResponseStatusException.class,
            () -> auth.login(new LoginRequest("test@example.com", "wrong"))).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> auth.login(new LoginRequest("test@example.com", "é".repeat(37)))).getStatusCode().value());
        verifyNoInteractions(encoder);
    }
    @Test void profileUpdatesPreserveRoleAndTrimNames() {
        var users = mock(UserRepository.class);
        User user = new User(); user.id = 7L; user.role = "USER";
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        var result = new UserService(users).update(7L, new UpdateProfileRequest(" New ", " Name "));
        assertEquals("New", result.firstName()); assertEquals("Name", result.lastName()); assertEquals("USER", result.role());
    }
}
