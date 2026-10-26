package com.aes.similarity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "similar_answer_results", uniqueConstraints = @UniqueConstraint(columnNames = {"first_evaluation_id", "second_evaluation_id"}), indexes = {
        @Index(name = "idx_similar_exam", columnList = "exam_id"),
        @Index(name = "idx_similar_question", columnList = "question_id"),
        @Index(name = "idx_similar_student", columnList = "first_student_id"),
        @Index(name = "idx_similar_compared_student", columnList = "second_student_id"),
        @Index(name = "idx_similar_severity", columnList = "severity"),
        @Index(name = "idx_similar_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class SimilarAnswerResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long examId;
    @Column(nullable = false) private Long questionId;
    @Column(nullable = false) private Long firstEvaluationId;
    @Column(nullable = false) private Long firstStudentId;
    @Column(nullable = false) private Long secondEvaluationId;
    @Column(nullable = false) private Long secondStudentId;
    @Column(nullable = false) private Double textSimilarity;
    @Column(nullable = false) private Double conceptSimilarity;
    @Column(nullable = false) private Double combinedSimilarity;
    @Column(nullable = false) private String severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SimilarityReviewStatus status = SimilarityReviewStatus.REVIEW_REQUIRED;
    private Instant reviewedAt;
    private Long reviewedBy;
    @Column(columnDefinition = "TEXT") private String reviewNotes;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
