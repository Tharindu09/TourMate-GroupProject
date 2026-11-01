package com.tourmate.service;

import com.tourmate.dto.UpdateProfileRequest;
import com.tourmate.dto.UserProfile;
import com.tourmate.model.User;
import com.tourmate.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) {this.users = users;}
    private User current(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
    public UserProfile profile(Long id) {return UserProfile.from(current(id));}
    public UserProfile update(Long id, UpdateProfileRequest body) {
        User user = current(id); user.firstName = body.firstName().strip(); user.lastName = body.lastName().strip();
        return UserProfile.from(users.save(user));
    }
}
