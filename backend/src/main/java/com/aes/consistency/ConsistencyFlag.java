package com.aes.consistency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "evaluation_consistency_flags", uniqueConstraints = @UniqueConstraint(columnNames = {"evaluation_id", "compared_evaluation_id"}), indexes = {
        @Index(name = "idx_consistency_exam", columnList = "exam_id"),
        @Index(name = "idx_consistency_question", columnList = "question_id"),
        @Index(name = "idx_consistency_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class ConsistencyFlag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long examId;
    @Column(nullable = false) private Long questionId;
    @Column(nullable = false) private Long evaluationId;
    @Column(nullable = false) private Long comparedEvaluationId;
    @Column(nullable = false) private Double semanticSimilarity;
    @Column(nullable = false) private Double conceptCoverageDifference;
    @Column(nullable = false) private Double marksDifference;
    @Column(nullable = false) private String severity;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConsistencyReviewStatus status = ConsistencyReviewStatus.REVIEW_REQUIRED;
    private Long reviewedBy;
    private Instant reviewedAt;
    @Column(columnDefinition = "TEXT") private String reviewNotes;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
