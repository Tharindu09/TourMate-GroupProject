package com.tourmate.controller;

import com.tourmate.dto.UpdateProfileRequest;
import com.tourmate.dto.UserProfile;
import com.tourmate.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService users;
    public UserController(UserService users) {this.users = users;}
    @GetMapping public UserProfile me(@AuthenticationPrincipal Jwt jwt) {return users.profile(Long.valueOf(jwt.getSubject()));}
    @PutMapping public UserProfile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest body) {
        return users.update(Long.valueOf(jwt.getSubject()), body);
    }
}
