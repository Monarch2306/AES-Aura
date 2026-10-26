package com.aes.answersheets;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerSheetRepository extends JpaRepository<AnswerSheet, Long> {
    List<AnswerSheet> findByExamId(Long examId);
    List<AnswerSheet> findByStudentId(Long studentId);
    long countByStatus(AnswerSheetStatus status);
}
