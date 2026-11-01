package com.tourmate.user;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, length=80) public String firstName;
    @Column(nullable=false, length=80) public String lastName;
    @Column(nullable=false, unique=true, length=254) public String email;
    @Column(nullable=false) public String passwordHash;
    @Column(length=1000) public String profileImageUrl;
    @Column(nullable=false, length=20) public String role = "USER";
    @Column(nullable=false, updatable=false) public Instant createdAt = Instant.now();
    @Column(nullable=false) public Instant updatedAt = Instant.now();
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
}
