package com.aes.exams.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ExamCreateRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Subject is required") String subject,
        @FutureOrPresent(message = "Exam date cannot be in the past") LocalDate examDate
) {}
