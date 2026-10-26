package com.aes.evaluations.dto;

import java.util.List;

public record CopilotResponse(
        Long evaluationId,
        Double provisionalScore,
        Double maximumMarks,
        Double confidence,
        Double semanticSimilarity,
        Double conceptCoverage,
        List<String> matchedConcepts,
        List<String> missingConcepts,
        List<String> contradictedConcepts,
        String answerQuality,
        List<String> strengths,
        List<String> weaknesses,
        String suggestedFeedback,
        String recommendation,
        String explanation
) {}
