package com.tourmate.controller;

import com.tourmate.dto.AuthResponse;
import com.tourmate.dto.LoginRequest;
import com.tourmate.dto.RegisterRequest;
import com.tourmate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) {this.auth = auth;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest body) {return auth.register(body);}
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) {return auth.login(body);}
}
