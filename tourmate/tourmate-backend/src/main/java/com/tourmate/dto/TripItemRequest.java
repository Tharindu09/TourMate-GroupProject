package com.tourmate.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
public record TripItemRequest(@NotNull @Positive Long dayId, @NotBlank @Size(max=300) String title,
 @NotNull @Pattern(regexp="HOTEL|RESTAURANT|ATTRACTION|ACTIVITY|TRANSPORT|OTHER") String category,
 @NotNull @Pattern(regexp="CUSTOM|TOURMATE|GEOAPIFY|RAPIDAPI") String placeSource,
 @Size(max=2000) String placeId, @Size(max=1000) String address,
 @DecimalMin("-90") @DecimalMax("90") Double latitude, @DecimalMin("-180") @DecimalMax("180") Double longitude,
 @NotNull LocalTime startTime, @NotNull LocalTime endTime,
 @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal estimatedCost,
 @NotNull @Size(max=4000) String notes) {}
