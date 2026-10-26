package com.aes.similarity;

public record SimilarAnswerResponse(
        Long id,
        Long questionId,
        Integer questionNumber,
        Long firstEvaluationId,
        Long firstStudentId,
        String firstAnswer,
        Long secondEvaluationId,
        Long secondStudentId,
        String secondAnswer,
        Double textSimilarity,
        Double conceptSimilarity,
        Double similarity,
        String severity,
        String status,
        String notice,
        String firstConcepts,
        String secondConcepts,
        String reviewNotes
) {}
