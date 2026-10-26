package com.aes.similarity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimilarAnswerResultRepository extends JpaRepository<SimilarAnswerResult, Long> {
    List<SimilarAnswerResult> findByExamIdOrderByCombinedSimilarityDesc(Long examId);
    Optional<SimilarAnswerResult> findByFirstEvaluationIdAndSecondEvaluationId(Long firstEvaluationId, Long secondEvaluationId);
}
