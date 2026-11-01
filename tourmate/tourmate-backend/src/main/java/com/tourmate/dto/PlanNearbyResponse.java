package com.tourmate.dto;
import java.time.*;
import java.util.List;
public record PlanNearbyResponse(RoutePoint anchor,LocalTime at,String context,String mode,List<Option> options,List<String>warnings) {
 public record Option(GeoPlace place,int travelMinutes,double distanceMeters) {}
}
