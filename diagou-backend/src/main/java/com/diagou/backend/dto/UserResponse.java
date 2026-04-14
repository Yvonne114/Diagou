package com.diagou.backend.dto;

import com.diagou.backend.model.UsersEntity;
import com.diagou.backend.model.enums.UserRole;
import com.diagou.backend.model.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String displayName;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;

    public static UserResponse from(UsersEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .displayName(user.getDisplayName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
