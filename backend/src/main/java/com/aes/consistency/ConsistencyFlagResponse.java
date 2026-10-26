package com.aes.consistency;

public record ConsistencyFlagResponse(
        Long id, Long questionId, Integer questionNumber,
        Long evaluationId, Long comparedEvaluationId,
        String evaluationAnswer, String comparedEvaluationAnswer,
        Double evaluationMarks, Double comparedEvaluationMarks,
        Double semanticSimilarity, Double conceptCoverageDifference,
        Double marksDifference, String severity, String reason,
        String status, String reviewNotes
) {}
