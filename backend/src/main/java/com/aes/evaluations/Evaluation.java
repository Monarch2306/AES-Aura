package com.aes.evaluations;

import com.aes.answersheets.AnswerSheet;
import com.aes.questions.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "evaluations", uniqueConstraints = @UniqueConstraint(columnNames = {"answer_sheet_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_sheet_id", nullable = false)
    private AnswerSheet answerSheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String studentAnswerText;

    @Column(nullable = false)
    private Double marksAwarded;

    @Column(nullable = false)
    private Double maxMarks;

    private Double normalizedScore;
    private Double tfidfSimilarity;
    private Double semanticSimilarity;
    private Double conceptCoverage;

    @Column(columnDefinition = "TEXT")
    private String matchedConcepts; // comma-separated

    @Column(columnDefinition = "TEXT")
    private String missingConcepts;

    @Column(columnDefinition = "TEXT")
    private String contradictedConcepts;

    private Double ocrConfidence;
    private Double segmentationConfidence;
    private Double overallConfidence;

    @Enumerated(EnumType.STRING)
    private ConfidenceLevel confidenceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String explanationJson;

    // Teacher override / review trail (Phase 11)
    private Double originalAiMarks;
    private Double finalMarks;
    private String overrideReason;
    private Long reviewedByUserId;
    private Instant reviewedAt;

    @Column(nullable = false, updatable = false)
    private Instant evaluatedAt = Instant.now();
}
