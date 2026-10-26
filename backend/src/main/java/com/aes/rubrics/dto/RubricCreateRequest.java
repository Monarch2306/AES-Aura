package com.aes.rubrics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RubricCreateRequest(
        @NotNull(message = "questionId is required") Long questionId,
        @NotBlank(message = "referenceAnswer is required") String referenceAnswer,
        @NotEmpty(message = "At least one concept is required") @Valid List<RubricConceptRequest> concepts,
        Integer minRequiredConcepts,
        Double weightSemantic,
        Double weightConceptCoverage
) {}
