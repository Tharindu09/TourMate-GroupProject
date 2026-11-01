package com.tourmate.user;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }
    private User current(Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
    @GetMapping public UserProfile me(@AuthenticationPrincipal Jwt jwt) { return UserProfile.from(current(jwt)); }
    public record UpdateProfile(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName) {}
    @PutMapping public UserProfile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfile body) {
        User user = current(jwt); user.firstName = body.firstName().strip(); user.lastName = body.lastName().strip();
        return UserProfile.from(users.save(user));
    }
}
