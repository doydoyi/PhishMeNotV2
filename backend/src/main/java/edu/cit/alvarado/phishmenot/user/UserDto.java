package edu.cit.alvarado.phishmenot.user;

import java.time.LocalDateTime;

/** Public view of a user account. Never exposes the Google subject identifier. */
public record UserDto(
        Long id,
        String name,
        String email,
        String profilePictureUrl,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getUserId(), user.getName(), user.getEmail(), user.getProfilePictureUrl(),
                user.getRole(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
