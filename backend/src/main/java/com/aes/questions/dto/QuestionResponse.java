package com.aes.questions.dto;

public record QuestionResponse(Long id, Long examId, Integer questionNumber, String text, Double maxMarks, boolean hasRubric) {}
