package com.tourmate.dto;

public record PlacePhoto(String url, String sourceUrl, String author, String license, String licenseUrl) {
    public static PlacePhoto unavailable() {return new PlacePhoto(null, null, null, null, null);}
}
