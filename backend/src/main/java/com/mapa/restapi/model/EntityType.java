package com.mapa.restapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"entityType","restaurant","hotel"})
public class EntityType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long entityID;

    @Enumerated(EnumType.STRING)
    private PlaceType entityType;

    @OneToOne(mappedBy = "entityID")
    private Restaurant restaurant;

    @OneToOne(mappedBy = "entityID")
    private Hotel hotel;

}
