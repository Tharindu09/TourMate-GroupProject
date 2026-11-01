package com.tourmate.dto;

import jakarta.validation.constraints.*;
public record UpdateProfileRequest(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName) {}
