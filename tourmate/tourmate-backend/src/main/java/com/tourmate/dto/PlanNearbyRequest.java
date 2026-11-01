package com.tourmate.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
public record PlanNearbyRequest(@NotNull @Valid ManualPlanRequest plan,@NotNull LocalDate date,@NotNull LocalTime at,@NotNull @Pattern(regexp="RESTAURANT|HOTEL")String category) {}
