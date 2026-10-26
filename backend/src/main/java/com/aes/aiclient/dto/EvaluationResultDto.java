package com.aes.aiclient.dto;

import java.util.List;
import java.util.Map;

public record EvaluationResultDto(
        Integer questionNumber,
        Double maxMarks,
        Double marksAwarded,
        Double normalizedScore,
        Double tfidfSimilarity,
        Double semanticSimilarity,
        Double conceptCoverage,
        Double hybridScore,
        List<String> matchedConcepts,
        List<String> missingConcepts,
        List<String> contradictedConcepts,
        List<Map<String, Object>> conceptDetails,
        Double ocrConfidence,
        Double segmentationConfidence,
        Double overallConfidence,
        String confidenceLevel,
        Map<String, Object> confidenceBreakdown,
        String status,
        List<String> reviewReasons,
        String feedback,
        Map<String, Object> explanation
) {}
