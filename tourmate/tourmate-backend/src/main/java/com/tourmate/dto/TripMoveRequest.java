package com.tourmate.dto;
import jakarta.validation.constraints.*;
public record TripMoveRequest(@NotNull @Positive Long dayId, @Min(0) @Max(100) int position) {}
