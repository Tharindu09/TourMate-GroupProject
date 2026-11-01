package com.tourmate.user;
public record UserProfile(Long id, String firstName, String lastName, String email, String profileImageUrl, String role) {
    public static UserProfile from(User user) {
        return new UserProfile(user.id, user.firstName, user.lastName, user.email, user.profileImageUrl, user.role);
    }
}
