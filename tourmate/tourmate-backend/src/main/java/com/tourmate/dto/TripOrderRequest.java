package com.tourmate.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record TripOrderRequest(@NotNull @Size(max=100) List<@NotNull @Positive Long> itemIds) {}
