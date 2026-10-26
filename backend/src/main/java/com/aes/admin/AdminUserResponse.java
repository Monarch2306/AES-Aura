package com.aes.admin;

import com.aes.common.Role;
import java.time.Instant;

public record AdminUserResponse(Long id, String name, String email, Role role, com.aes.users.UserStatus status, Instant createdAt) {
    public AdminUserResponse(Long id, String name, String email, Role role, Instant createdAt) {
        this(id, name, email, role, com.aes.users.UserStatus.ACTIVE, createdAt);
    }
}
