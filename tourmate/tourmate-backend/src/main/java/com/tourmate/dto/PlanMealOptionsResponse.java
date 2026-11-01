package com.tourmate.dto;
import java.time.LocalTime;
import java.util.List;
public record PlanMealOptionsResponse(List<Meal> meals) {
 public record Meal(String key,String mealType,LocalTime at,PlanNearbyResponse suggestions,String error) {}
}
