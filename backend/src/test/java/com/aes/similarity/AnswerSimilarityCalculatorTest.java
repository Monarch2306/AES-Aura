package com.aes.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerSimilarityCalculatorTest {
    @Test
    void identicalNormalizedAnswersAreHighlySimilar() {
        double score = AnswerSimilarityCalculator.tfIdfCosine(
                "Normalization removes duplicate data.",
                "normalization removes duplicate data");

        assertTrue(score > 0.99);
    }

    @Test
    void unrelatedAnswersAreNotHighlySimilar() {
        double score = AnswerSimilarityCalculator.tfIdfCosine(
                "Normalization reduces database redundancy.",
                "Photosynthesis converts sunlight into chemical energy.");

        assertTrue(score < 0.2);
    }

    @Test
    void emptyAnswersDoNotProduceEvidence() {
        assertTrue(AnswerSimilarityCalculator.tfIdfCosine("", "database normalization") == 0.0);
        assertTrue(AnswerSimilarityCalculator.conceptOverlap("", "normalization") == 0.0);
    }
}