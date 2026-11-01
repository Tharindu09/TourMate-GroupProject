package com.tourmate.dto;

import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=8, max=72) String password) {}
