package com.aes.evaluations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByAnswerSheetId(Long answerSheetId);
    Optional<Evaluation> findByAnswerSheetIdAndQuestionId(Long answerSheetId, Long questionId);
    List<Evaluation> findByAnswerSheet_Exam_Id(Long examId);
    List<Evaluation> findByAnswerSheet_Student_Id(Long studentId);
    List<Evaluation> findByQuestionId(Long questionId);
    long countByStatus(EvaluationStatus status);
}
