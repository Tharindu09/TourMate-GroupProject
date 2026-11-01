package com.tourmate.dto;

import java.util.List;
public record TravelPlace(String id, String type, String provider, String name, String description,
    String address, Double latitude, Double longitude, String photoUrl, String photoCredit,
    Double rating, Integer ratingScale, Integer reviewCount, String phone, String website,
    String sourceUrl, List<String> openingHours, List<String> amenities, List<Review> reviews,
    String priceText, String currency, String resultToken) {
    public record Review(String title, String text, String author, Double rating, String date, String url) {}
}
