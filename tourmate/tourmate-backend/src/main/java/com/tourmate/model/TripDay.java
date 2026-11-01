package com.tourmate.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="trip_days")
public class TripDay {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long tripId;
 @Column(nullable=false) public int dayNumber;
 @Column(nullable=false) public LocalDate date;
}
