package com.aes.aiclient.dto;

import java.util.List;

public record OcrResultDto(
        String text,
        Double confidence,
        List<String> lines,
        List<WordBoxDto> words,
        String bestPreprocessing,
        Boolean needsReview
) {}
