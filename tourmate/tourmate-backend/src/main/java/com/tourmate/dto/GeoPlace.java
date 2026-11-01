package com.tourmate.dto;

import java.util.List;
public record GeoPlace(String id, String name, String address, double latitude, double longitude,
    List<String> categories, String website, String phone, String openingHours, String description, String wheelchair, GeoMedia media) {}
