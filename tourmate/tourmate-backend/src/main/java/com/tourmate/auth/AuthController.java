package com.tourmate.auth;
import com.tourmate.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    public AuthController(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users; this.passwords = passwords; this.encoder = encoder;
    }
    public record Register(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=8, max=72) String password) {}
    public record Login(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody Register body) {
        String email = body.email().strip().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        checkPassword(body.password());
        User user = new User(); user.firstName = body.firstName().strip(); user.lastName = body.lastName().strip();
        user.email = email; user.passwordHash = passwords.encode(body.password());
        return token(users.saveAndFlush(user));
    }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody Login body) {
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
