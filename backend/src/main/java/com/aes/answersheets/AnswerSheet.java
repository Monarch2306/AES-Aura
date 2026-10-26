package com.aes.answersheets;

import com.aes.exams.Exam;
import com.aes.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "answer_sheets")
@Getter
@Setter
@NoArgsConstructor
public class AnswerSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFilePath;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerSheetStatus status = AnswerSheetStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String rawOcrText;

    private Double ocrConfidence;

    private Double imageQualityScore;

    private Double imageBlurScore;

    private Double imageBrightnessScore;

    private Double imageContrastScore;

    private Integer imageResolutionWidth;

    private Integer imageResolutionHeight;

    private Boolean imageRotationDetected;

    @Column(columnDefinition = "TEXT")
    private String imageQualityWarnings;

    @Column(columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "total_pages")
    private Integer totalPages = 1;

    @OneToMany(mappedBy = "answerSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageNumber ASC")
    private java.util.List<AnswerPage> pages = new java.util.ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    private Instant processedAt;
}
