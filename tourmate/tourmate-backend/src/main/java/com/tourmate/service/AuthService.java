package com.tourmate.service;

import com.tourmate.dto.AuthResponse;
import com.tourmate.dto.LoginRequest;
import com.tourmate.dto.RegisterRequest;
import com.tourmate.dto.UserProfile;
import com.tourmate.model.User;
import com.tourmate.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users; this.passwords = passwords; this.encoder = encoder;
    }
    public AuthResponse register(RegisterRequest body) {
        String email = body.email().strip().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        checkPassword(body.password());
        User user = new User(); user.firstName = body.firstName().strip(); user.lastName = body.lastName().strip();
        user.email = email; user.passwordHash = passwords.encode(body.password());
        return token(users.saveAndFlush(user));
    }
    public AuthResponse login(LoginRequest body) {
        checkPassword(body.password());
        User user = users.findByEmail(body.email().strip().toLowerCase(Locale.ROOT))
            .filter(u -> passwords.matches(body.password(), u.passwordHash))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return token(user);
    }
    private void checkPassword(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
    }
    private AuthResponse token(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("tourmate").subject(user.id.toString())
            .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("role", user.role).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", 3600, UserProfile.from(user));
    }
}
