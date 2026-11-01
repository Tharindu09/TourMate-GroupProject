package com.tourmate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="places")
public class Place {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long destinationId;
    @Column(nullable=false, length=160) public String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) public PlaceType type;
    @Column(nullable=false, columnDefinition="TEXT") public String description;
    @Column(nullable=false, length=500) public String address;
    @Column(nullable=false) public double latitude;
    @Column(nullable=false) public double longitude;
    @Column(nullable=false, precision=3, scale=2) public BigDecimal averageRating = BigDecimal.ZERO;
    @Column(nullable=false) public int reviewCount;
    @Column(nullable=false) public int priceLevel;
    @Column(nullable=false, precision=12, scale=2) public BigDecimal estimatedCost;
    @Column(length=40) public String phone;
    @Column(length=1000) public String website;
    @Column(length=1000) public String coverImageUrl;
}
