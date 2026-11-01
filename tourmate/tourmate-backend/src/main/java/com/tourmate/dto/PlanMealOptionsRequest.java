package com.tourmate.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record PlanMealOptionsRequest(@NotNull @Valid ManualPlanRequest plan,@NotNull LocalDate date) {}
