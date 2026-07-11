package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.Role;
import com.oglimmer.wiki.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String email, String displayName, Role role, UserStatus status, Instant createdAt) {

    public static UserDto from(AppUser user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
