package com.aes.evaluations.dto;

import java.time.Instant;

public record EvaluationProgressEvent(
        Long sheetId,
        String stage,
        int stepIndex,
        int progressPercent,
        String message,
        String timestamp
) {
    public static EvaluationProgressEvent of(Long sheetId, String stage, int stepIndex, int progressPercent, String message) {
        return new EvaluationProgressEvent(sheetId, stage, stepIndex, progressPercent, message, Instant.now().toString());
    }
}