package com.aes.answersheets.dto;

import java.time.Instant;

public record AnswerPageResponse(
        Long id,
        Long answerSheetId,
        Integer pageNumber,
        String rawOcrText,
        Double ocrConfidence,
        Instant createdAt
) {}