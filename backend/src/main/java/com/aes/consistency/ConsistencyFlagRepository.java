package com.aes.consistency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsistencyFlagRepository extends JpaRepository<ConsistencyFlag, Long> {
    List<ConsistencyFlag> findByExamIdOrderByMarksDifferenceDesc(Long examId);
    Optional<ConsistencyFlag> findByEvaluationIdAndComparedEvaluationId(Long evaluationId, Long comparedEvaluationId);
}
