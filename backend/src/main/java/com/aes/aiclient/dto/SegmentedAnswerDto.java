package com.aes.aiclient.dto;

import java.util.Map;

public record SegmentedAnswerDto(Integer questionNumber, String studentAnswer, Double ocrConfidence, Map<String, Object> boundingBox) {}
