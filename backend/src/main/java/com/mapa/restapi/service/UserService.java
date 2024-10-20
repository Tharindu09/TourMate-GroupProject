package com.mapa.restapi.service;

import com.mapa.restapi.dto.UserDto;
import com.mapa.restapi.model.User;
import com.mapa.restapi.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private  UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public UserDto saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        if(userRepo.findByEmail(user.getEmail()).isPresent()) {
            return convertUserDto(user);
        }
        return null;

    }

    public void deleteUser(long id) {
        userRepo.deleteById(id);
    }

    public User findByEmail(String email) {

        return userRepo.findByEmail(email).orElse(null);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User updateUserByEmail(String email, User updatedUser) {
        Optional<User> user = userRepo.findByEmail(email);
        if (user.isPresent()) {
            User existingUser = user.get();
            // Selectively update fields
            if (updatedUser.getFirstname() != null) {
                existingUser.setFirstname(updatedUser.getFirstname());
            }

            if (updatedUser.getLastname() != null) {
                existingUser.setLastname(updatedUser.getLastname());
            }

            if (updatedUser.getIdentifier() != null) {
                existingUser.setIdentifier(updatedUser.getIdentifier());
            }
            existingUser.setUsertype(updatedUser.getUsertype());
            return userRepo.save(existingUser);
        }
        throw new RuntimeException("User not found");
    }

    public User findUserByUserIdentifier(String identifier) {
        return  userRepo.getByIdentifier(identifier).orElse(null);
    }

    public  UserDto convertUserDto(User user){
        return UserDto.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .identifier(user.getIdentifier())
                .usertype(user.getUsertype())
        .build();
    }
}

