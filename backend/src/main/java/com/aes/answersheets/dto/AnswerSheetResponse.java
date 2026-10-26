package com.aes.answersheets.dto;

import com.aes.answersheets.AnswerSheetStatus;

import java.time.Instant;
import java.util.List;

public record AnswerSheetResponse(
        Long id, Long examId, Long studentId, String studentName,
        String originalFileName, String mimeType, Long fileSizeBytes,
        AnswerSheetStatus status, String rawOcrText, Double ocrConfidence, String processingError,
        Instant uploadedAt, Instant processedAt,
        Double imageQualityScore, Double imageBlurScore, Double imageBrightnessScore,
        Double imageContrastScore, Integer imageResolutionWidth, Integer imageResolutionHeight,
        Boolean imageRotationDetected, List<String> imageQualityWarnings
) {}
