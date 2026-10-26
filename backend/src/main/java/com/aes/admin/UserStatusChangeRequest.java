package com.aes.admin;

import com.aes.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusChangeRequest(
        @NotNull UserStatus status
) {}
