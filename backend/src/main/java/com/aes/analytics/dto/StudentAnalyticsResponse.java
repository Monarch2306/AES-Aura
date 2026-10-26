package com.aes.analytics.dto;

import java.util.List;
import java.util.Map;

public record StudentAnalyticsResponse(
        Long studentId,
        Long totalAnswerSheets,
        Double averageMarksPercent,
        Double highestScorePercent,
        Double recentScorePercent,
        Double improvementPercent,
        Map<String, Double> subjectAveragePercent,
        List<String> topWeakConcepts,
        List<String> strongConcepts,
        Map<String, Double> conceptMasteryPercent,
        List<LearningRecommendation> recommendations,
        List<ProgressPoint> progressOverTime
) {
    public record ProgressPoint(String examTitle, java.time.Instant date, Double marksPercent) {}
}
