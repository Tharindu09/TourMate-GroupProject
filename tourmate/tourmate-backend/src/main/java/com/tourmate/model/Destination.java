package com.tourmate.model;

import jakarta.persistence.*;
@Entity @Table(name="destinations")
public class Destination {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, length=120) public String name;
    @Column(nullable=false, length=120) public String city;
    @Column(nullable=false, length=120) public String country;
    @Column(nullable=false, columnDefinition="TEXT") public String description;
    @Column(length=1000) public String coverImageUrl;
    @Column(nullable=false) public double latitude;
    @Column(nullable=false) public double longitude;
}
