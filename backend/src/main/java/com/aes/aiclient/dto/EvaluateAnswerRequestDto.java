package com.aes.aiclient.dto;

public record EvaluateAnswerRequestDto(
        String studentAnswer,
        Double ocrConfidence,
        RubricDto rubric,
        Double segmentationConfidence
) {}
