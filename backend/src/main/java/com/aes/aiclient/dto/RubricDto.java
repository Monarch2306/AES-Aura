package com.aes.aiclient.dto;

import java.util.List;

public record RubricDto(
        Integer questionNumber,
        Double maxMarks,
        String referenceAnswer,
        List<RubricConceptDto> concepts,
        Integer minRequiredConcepts,
        Double weightSemantic,
        Double weightConceptCoverage
) {}
