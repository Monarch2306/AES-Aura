package com.aes.evaluations.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Teacher manually sets the final marks, overriding the AI score. */
public record OverrideRequest(
        @NotNull @PositiveOrZero(message = "finalMarks must be >= 0") Double finalMarks,
        @NotNull(message = "A reason is required when overriding an AI score") String overrideReason,
        String updatedFeedback
) {}
