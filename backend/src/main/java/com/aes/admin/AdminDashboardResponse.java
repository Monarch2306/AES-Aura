package com.aes.admin;

public record AdminDashboardResponse(
        Long users,
        Long students,
        Long teachers,
        Long admins,
        Long exams,
        Long questions,
        Long answerSheets,
        Long evaluations,
        Long pendingReviews,
        Long failedEvaluations,
        Long similarityFlags,
        Long consistencyFlags,
        Long learningRecommendations
) {}
