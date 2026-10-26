package com.aes.rubrics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rubric_concepts")
@Getter
@Setter
@NoArgsConstructor
public class RubricConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id", nullable = false)
    private Rubric rubric;

    @Column(nullable = false)
    private String concept;

    @Column(nullable = false)
    private Double weight = 1.0;

    @Column(nullable = false)
    private boolean required = false;

    @Column(nullable = false)
    private boolean negative = false;
}
