package com.aes.rubrics;

import com.aes.questions.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rubrics")
@Getter
@Setter
@NoArgsConstructor
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(nullable = false)
    private Integer minRequiredConcepts = 0;

    private Double weightSemantic;   // null => use AI-service global default
    private Double weightConceptCoverage;

    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RubricConcept> concepts = new ArrayList<>();
}
