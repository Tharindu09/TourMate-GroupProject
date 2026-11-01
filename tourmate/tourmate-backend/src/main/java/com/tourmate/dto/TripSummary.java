package com.tourmate.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TripSummary(Long id, String name, LocalDate startDate, LocalDate endDate, BigDecimal budget, int travelers, String status) {}
