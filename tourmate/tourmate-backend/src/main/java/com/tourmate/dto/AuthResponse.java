package com.tourmate.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {}
