package com.aes.answersheets;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "answer_pages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"answer_sheet_id", "page_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class AnswerPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_sheet_id", nullable = false)
    private AnswerSheet answerSheet;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "raw_ocr_text", columnDefinition = "TEXT")
    private String rawOcrText;

    @Column(name = "ocr_confidence")
    private Double ocrConfidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}