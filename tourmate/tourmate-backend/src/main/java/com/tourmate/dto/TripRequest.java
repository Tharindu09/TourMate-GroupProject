package com.tourmate.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
public record TripRequest(@NotBlank @Size(max=160) String name, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
 @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal budget,
 @Min(1) @Max(100) int travelers, @NotNull @Size(max=20) Set<@NotNull @Positive Long> destinationIds,
 @NotNull @Pattern(regexp="DRAFT|PLANNED|COMPLETED") String status) {}
