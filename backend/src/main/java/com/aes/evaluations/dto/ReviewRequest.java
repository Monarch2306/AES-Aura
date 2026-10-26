package com.aes.evaluations.dto;

import jakarta.validation.constraints.NotBlank;

/** Teacher approves or rejects an auto-evaluation without changing marks. */
public record ReviewRequest(
        @NotBlank(message = "action is required, e.g. APPROVE or REJECT") String action,
        String comment
) {}
