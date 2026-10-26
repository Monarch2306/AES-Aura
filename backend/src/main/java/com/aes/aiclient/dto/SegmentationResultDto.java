package com.aes.aiclient.dto;

import java.util.List;

public record SegmentationResultDto(List<SegmentedAnswerDto> answers, String pattern) {}
