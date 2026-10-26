package com.aes.rubrics.dto;

import java.util.List;

public record RubricResponse(
        Long id, Long questionId, String referenceAnswer,
        List<RubricConceptRequest> concepts,
        Integer minRequiredConcepts, Double weightSemantic, Double weightConceptCoverage
) {}
