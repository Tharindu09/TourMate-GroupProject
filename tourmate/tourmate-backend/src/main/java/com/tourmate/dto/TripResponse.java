package com.tourmate.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public record TripResponse(Long id, String name, LocalDate startDate, LocalDate endDate, BigDecimal budget,
 int travelers, String planningMode, String status, Set<Long> destinationIds,
 BigDecimal estimatedTotal, BigDecimal remainingBudget, Map<String,BigDecimal> costByCategory, List<Day> days, String planningPreferences) {
 public record Day(Long id, int dayNumber, LocalDate date, List<Item> items) {}
 public record Item(Long id, Long dayId, String title, String category, String placeSource, String placeId, String address,
  Double latitude, Double longitude, LocalTime startTime, LocalTime endTime, BigDecimal estimatedCost, String notes, int position, String mealType) {}
}
