package com.aes.aiclient.dto;

import java.util.List;

public record ImageQualityDto(
        Double qualityScore,
        Double blurScore,
        Double brightnessScore,
        Double contrastScore,
        Integer resolutionWidth,
        Integer resolutionHeight,
        Boolean rotationDetected,
        List<String> warnings
) {}
