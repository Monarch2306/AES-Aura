package com.aes.analytics.dto;

import java.util.List;
import java.util.Map;

public record ExamAnalyticsResponse(
        Long examId,
        Long studentCount,
        Long evaluatedStudentCount,
        Long evaluatedAnswerCount,
        Double averagePercent,
        Double medianPercent,
        Double highestPercent,
        Double lowestPercent,
        Map<String, Long> scoreDistribution,
        List<QuestionAnalytics> questions,
        Map<String, Double> conceptMasteryPercent,
        List<String> strongConcepts,
        List<String> weakConcepts,
        List<String> insights
) {
    public record QuestionAnalytics(
            Long questionId,
            Integer questionNumber,
            String questionText,
            Double maxMarks,
            Long attempts,
            Double averageMarks,
            Double averagePercent,
            String difficulty,
            Double conceptCoveragePercent,
            Double lowScorePercent,
            Double highScorePercent,
            List<Double> scorePercentages
    ) {}
}