package com.aes.questions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuestionCreateRequest(
        @NotNull(message = "examId is required") Long examId,
        @NotNull @Positive(message = "questionNumber must be positive") Integer questionNumber,
        @NotBlank(message = "Question text is required") String text,
        @NotNull @Positive(message = "maxMarks must be positive") Double maxMarks
) {}
