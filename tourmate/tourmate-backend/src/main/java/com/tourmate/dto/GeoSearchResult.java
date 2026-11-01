package com.tourmate.dto;

import java.util.List;
public record GeoSearchResult(List<GeoPlace> places, int page, boolean hasMore,
    double latitude, double longitude, String location) {}
