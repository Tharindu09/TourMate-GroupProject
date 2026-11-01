package com.tourmate.dto;
import java.util.List;
public record RouteEstimate(int minutes,double distanceMeters,List<RoutePoint> geometry,List<Double> elapsedSeconds) {}
