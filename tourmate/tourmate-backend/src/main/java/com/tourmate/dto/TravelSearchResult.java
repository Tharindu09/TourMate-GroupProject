package com.tourmate.dto;

import java.util.List;
public record TravelSearchResult(List<TravelPlace> places, int page, boolean hasMore,
    double latitude, double longitude, String location) {}
