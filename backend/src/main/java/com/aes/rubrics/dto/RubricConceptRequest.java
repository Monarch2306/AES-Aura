package com.aes.rubrics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RubricConceptRequest(
        @NotBlank(message = "Concept text is required") String concept,
        @NotNull @Positive(message = "weight must be positive") Double weight,
        boolean required,
        boolean negative
) {}
