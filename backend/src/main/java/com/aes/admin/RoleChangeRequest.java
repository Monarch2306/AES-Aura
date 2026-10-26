package com.aes.admin;

import com.aes.common.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull Role role) {}
