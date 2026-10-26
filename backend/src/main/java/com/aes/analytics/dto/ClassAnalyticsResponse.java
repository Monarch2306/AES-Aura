package com.aes.analytics.dto;

import java.util.List;

public record ClassAnalyticsResponse(
        Long examId,
        Long totalAnswerSheets,
        Long evaluatedCount,
        Long pendingReviewCount,
        Double mean,
        Double median,
        Double highest,
        Double lowest,
        Double averageConfidence,
        Double manualReviewRatePercent,
        List<QuestionStat> questionWisePerformance
) {
        public record QuestionStat(Long questionId, Integer questionNumber, Double averageMarks, Double maxMarks,
                                                                Double difficultyEstimate, Long attempts, String difficultyClassification,
                                                                List<String> commonMissingConcepts) {}
}
