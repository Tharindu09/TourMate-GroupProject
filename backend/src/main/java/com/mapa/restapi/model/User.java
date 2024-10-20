package com.mapa.restapi.model;

import jakarta.persistence.*;
import lombok.*;

/*
User Entity
PK : id (Auto Increment)
 */

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "forgotPassword"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userid;
    private String firstname;

    private String lastname;

    @Column(unique=true)
    private String email;

    private String password;
    private String gender;
    private int age;
    private String usertype;
    private String identifier;


    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private ForgotPassword forgotPassword;


    
}
