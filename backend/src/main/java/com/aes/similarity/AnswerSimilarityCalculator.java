package com.aes.similarity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final @SuppressWarnings("null")
class AnswerSimilarityCalculator {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "in",
            "is", "it", "of", "on", "or", "that", "the", "this", "to", "was", "were", "with"
    );

    private AnswerSimilarityCalculator() {}

    public static double tfIdfCosine(String first, String second) {
        Map<String, Integer> firstTerms = termFrequencies(first);
        Map<String, Integer> secondTerms = termFrequencies(second);
        if (firstTerms.isEmpty() || secondTerms.isEmpty()) return 0.0;

        Set<String> vocabulary = new HashSet<>(firstTerms.keySet());
        vocabulary.addAll(secondTerms.keySet());
        double dot = 0.0;
        double firstMagnitude = 0.0;
        double secondMagnitude = 0.0;
        for (String term : vocabulary) {
            double idf = Math.log(1.0 + 2.0 / (1.0 + documentFrequency(term, firstTerms, secondTerms)));
            double firstWeight = firstTerms.getOrDefault(term, 0) * idf;
            double secondWeight = secondTerms.getOrDefault(term, 0) * idf;
            dot += firstWeight * secondWeight;
            firstMagnitude += firstWeight * firstWeight;
            secondMagnitude += secondWeight * secondWeight;
        }
        if (firstMagnitude == 0.0 || secondMagnitude == 0.0) return 0.0;
        return clamp(dot / (Math.sqrt(firstMagnitude) * Math.sqrt(secondMagnitude)));
    }

    public static double conceptOverlap(String first, String second) {
        Set<String> left = termFrequencies(first).keySet();
        Set<String> right = termFrequencies(second).keySet();
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return clamp((double) intersection.size() / union.size());
    }

    public static double combined(double textSimilarity, double conceptSimilarity, double textWeight) {
        return clamp(textSimilarity * textWeight + conceptSimilarity * (1.0 - textWeight));
    }

    private static Map<String, Integer> termFrequencies(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(normalized.trim().isEmpty() ? new String[0] : normalized.trim().split("\\s+"))
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .collect(Collectors.toMap(token -> token, token -> 1, Integer::sum, HashMap::new));
    }

    private static int documentFrequency(String term, Map<String, Integer> first, Map<String, Integer> second) {
        return (first.containsKey(term) ? 1 : 0) + (second.containsKey(term) ? 1 : 0);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}