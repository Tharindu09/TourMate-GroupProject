package com.tourmate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record ManualPlanRequest(@NotNull @Valid TripRequest trip,
 @NotBlank @Size(max=300) String startingLocation,
 @NotNull @Pattern(regexp="CAR|PUBLIC_TRANSPORT|WALKING|CYCLING|OTHER") String transportMode,
 @Min(0) @Max(240) int transferMinutes,
 @NotNull @Size(min=1,max=60) List<@NotNull @Valid Day> days,
 @DecimalMin("-90") @DecimalMax("90") Double startingLatitude,
 @DecimalMin("-180") @DecimalMax("180") Double startingLongitude, Boolean optimizeTravel) {
 public ManualPlanRequest(TripRequest trip,String startingLocation,String transportMode,int transferMinutes,List<Day> days){this(trip,startingLocation,transportMode,transferMinutes,days,null,null,false);}
 public ManualPlanRequest(TripRequest trip,String startingLocation,String transportMode,int transferMinutes,List<Day> days,Double startingLatitude,Double startingLongitude){this(trip,startingLocation,transportMode,transferMinutes,days,startingLatitude,startingLongitude,false);}
 public record Day(@NotNull LocalDate date, @NotBlank @Size(max=300) String area,
  @NotNull LocalTime startTime, @NotNull LocalTime endTime,
  @NotNull @Size(max=30) List<@NotNull @Valid Stop> stops) {}
 public record Stop(@NotBlank @Size(max=120) String key, @NotBlank @Size(max=300) String title,
  @NotNull @Pattern(regexp="HOTEL|RESTAURANT|ATTRACTION|ACTIVITY|TRANSPORT|OTHER") String category,
  @NotNull @Pattern(regexp="CUSTOM|TOURMATE|GEOAPIFY|RAPIDAPI") String placeSource,
  @Size(max=2000) String placeId, @Size(max=1000) String address,
  @DecimalMin("-90") @DecimalMax("90") Double latitude, @DecimalMin("-180") @DecimalMax("180") Double longitude,
  @Min(5) @Max(720) int durationMinutes, LocalTime preferredTime,
  @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal estimatedCost,
  @NotNull @Size(max=4000) String notes,
  @NotNull @Pattern(regexp="NONE|BREAKFAST|LUNCH|DINNER") String mealType) {}
}
