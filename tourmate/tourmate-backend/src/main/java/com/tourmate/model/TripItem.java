package com.tourmate.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;
@Entity @Table(name="trip_items")
public class TripItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long dayId;
 @Column(nullable=false, length=300) public String title;
 @Column(nullable=false, length=20) public String category;
 @Column(nullable=false, length=20) public String mealType = "NONE";
 @Column(nullable=false, length=20) public String placeSource;
 @Column(length=2000) public String placeId;
 @Column(length=1000) public String address;
 public Double latitude;
 public Double longitude;
 @Column(nullable=false) public LocalTime startTime;
 @Column(nullable=false) public LocalTime endTime;
 @Column(nullable=false, precision=12, scale=2) public BigDecimal estimatedCost;
 @Column(nullable=false, length=4000) public String notes;
 @Column(nullable=false, name="sort_order") public int position;
}
