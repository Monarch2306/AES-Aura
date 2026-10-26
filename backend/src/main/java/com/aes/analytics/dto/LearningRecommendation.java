package com.aes.analytics.dto;

import java.util.List;

public record LearningRecommendation(
        String concept,
        String priority,
        Double masteryPercent,
        Long attempts,
        String reason,
        List<String> actions
) {}
