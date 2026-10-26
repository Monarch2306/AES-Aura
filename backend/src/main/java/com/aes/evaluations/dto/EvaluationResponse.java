package com.aes.evaluations.dto;

import com.aes.evaluations.ConfidenceLevel;
import com.aes.evaluations.EvaluationStatus;

import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        Long id, Long answerSheetId, Long questionId, Integer questionNumber,
        String studentAnswerText, Double marksAwarded, Double maxMarks, Double normalizedScore,
        Double tfidfSimilarity, Double semanticSimilarity, Double conceptCoverage,
        List<String> matchedConcepts, List<String> missingConcepts, List<String> contradictedConcepts,
        Double ocrConfidence, Double segmentationConfidence, Double overallConfidence,
        ConfidenceLevel confidenceLevel, EvaluationStatus status, String feedback,
        Double originalAiMarks, Double finalMarks, String overrideReason,
        Long reviewedByUserId, Instant reviewedAt, Instant evaluatedAt
) {}
