package com.mapa.restapi.dto;


import com.mapa.restapi.model.User;
import com.mapa.restapi.service.UserService;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private long userid;
    private String firstname;
    private String lastname;
    private String email;
    //private String gender;
    //private int age;
    private String identifier;
    private String usertype;



}
