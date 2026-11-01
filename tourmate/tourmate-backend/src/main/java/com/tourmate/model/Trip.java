package com.tourmate.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Table(name="trips")
public class Trip {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long userId;
 @Column(nullable=false, length=160) public String name;
 @Column(nullable=false) public LocalDate startDate;
 @Column(nullable=false) public LocalDate endDate;
 @Column(nullable=false, precision=12, scale=2) public BigDecimal budget;
 @Column(nullable=false) public int travelers;
 @Column(nullable=false, length=20) public String planningMode = "MANUAL";
 @Column(nullable=false, length=20) public String status = "DRAFT";
 @Column(nullable=false) public Instant createdAt = Instant.now();
 @Column(nullable=false) public Instant updatedAt = Instant.now();
 @Column(columnDefinition="LONGTEXT") public String planningPreferences;
 @ElementCollection
 @CollectionTable(name="trip_destinations", joinColumns=@JoinColumn(name="trip_id"))
 @Column(name="destination_id") public Set<Long> destinationIds = new LinkedHashSet<>();
}
