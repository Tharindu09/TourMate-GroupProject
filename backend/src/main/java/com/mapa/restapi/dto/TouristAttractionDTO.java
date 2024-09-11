package com.mapa.restapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TouristAttractionDTO {

    private long id;
    private String type;
    private String description;
    private String name;
    private String city;
    private String imgUrl;
    private String latitude;
    private String longitude;
    private String rating;
    private String web_url;
    private String phone;
    private String address;


}


