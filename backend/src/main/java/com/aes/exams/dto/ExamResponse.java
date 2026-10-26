package com.aes.exams.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ExamResponse(
        Long id, String title, String subject, LocalDate examDate,
        Long teacherId, String teacherName, Instant createdAt
) {}
