package com.aes.questions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamIdOrderByQuestionNumberAsc(Long examId);
    Optional<Question> findByExamIdAndQuestionNumber(Long examId, Integer questionNumber);
}
