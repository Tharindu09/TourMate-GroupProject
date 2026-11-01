package com.tourmate.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record ManualPlanPreview(List<Day> days, List<Unscheduled> unscheduled, List<String> warnings, BigDecimal estimatedTotal, BigDecimal remainingBudget, Optimization optimization) {
 public ManualPlanPreview(List<Day> days,List<Unscheduled> unscheduled,List<String>warnings,BigDecimal total,BigDecimal remaining){this(days,unscheduled,warnings,total,remaining,null);}
 public record Optimization(boolean enabled,int beforeTravelMinutes,int afterTravelMinutes,boolean searchLimited,String description) {}
 public record Day(LocalDate date, String area, List<Visit> visits, RoutePoint origin, List<TravelLeg> travelLegs) {}
 public record TravelLeg(RoutePoint from, RoutePoint to, LocalTime departureTime, LocalTime arrivalTime, String mode, RouteEstimate route) {}
 public record Visit(ManualPlanRequest.Stop stop, LocalTime startTime, LocalTime endTime) {}
 public record Unscheduled(LocalDate date, String title, String reason) {}
}
