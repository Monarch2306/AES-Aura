package com.aes.analytics;

import com.aes.analytics.dto.ClassAnalyticsResponse;
import com.aes.analytics.dto.StudentAnalyticsResponse;
import com.aes.analytics.dto.LearningRecommendation;
import com.aes.analytics.dto.ExamAnalyticsResponse;
import com.aes.answersheets.AnswerSheet;
import com.aes.answersheets.AnswerSheetRepository;
import com.aes.answersheets.AnswerSheetStatus;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import com.aes.evaluations.Evaluation;
import com.aes.evaluations.EvaluationRepository;
import com.aes.evaluations.EvaluationStatus;
import com.aes.questions.Question;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class AnalyticsService {

    private final EvaluationRepository evaluationRepository;
    private final AnswerSheetRepository answerSheetRepository;
        private final ExamService examService;
        private final ResourceAuthorizationService authorizationService;
        private final int questionDifficultyMinimumSample;

        public AnalyticsService(EvaluationRepository evaluationRepository, AnswerSheetRepository answerSheetRepository,
                                                        ExamService examService, ResourceAuthorizationService authorizationService,
                                                        @Value("${app.analytics.question-minimum-sample-size:3}") int questionDifficultyMinimumSample) {
        this.evaluationRepository = evaluationRepository;
        this.answerSheetRepository = answerSheetRepository;
                this.examService = examService;
                this.authorizationService = authorizationService;
                this.questionDifficultyMinimumSample = questionDifficultyMinimumSample;
    }

        public StudentAnalyticsResponse studentAnalytics(Long studentId, UserPrincipal principal) {
                authorizationService.requireStudentSelfOrAdmin(studentId, principal);
        List<AnswerSheet> sheets = answerSheetRepository.findByStudentId(studentId);
        List<Evaluation> evaluations = evaluationRepository.findByAnswerSheet_Student_Id(studentId);

        double avgPercent = evaluations.isEmpty() ? 0.0 : evaluations.stream()
                .mapToDouble(this::scorePercent)
                .average().orElse(0.0);
        double highestPercent = evaluations.stream().mapToDouble(this::scorePercent).max().orElse(0.0);
        List<Evaluation> chronological = evaluations.stream()
                .sorted(Comparator.comparing(Evaluation::getEvaluatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        double recentPercent = chronological.isEmpty() ? 0.0 : scorePercent(chronological.get(chronological.size() - 1));
        double improvementPercent = chronological.size() < 2 ? 0.0
                : round2(recentPercent - scorePercent(chronological.get(0)));

        Map<String, List<Evaluation>> bySubject = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getAnswerSheet().getExam().getSubject()));
        Map<String, Double> subjectAvg = new LinkedHashMap<>();
        bySubject.forEach((subject, evals) -> subjectAvg.put(subject, round2(
                evals.stream().mapToDouble(this::scorePercent).average().orElse(0.0)
        )));

        // Weakness = concepts that appear most often in "missingConcepts"
        Map<String, Long> missingFrequency = new HashMap<>();
        for (Evaluation e : evaluations) {
            if (e.getMissingConcepts() == null || e.getMissingConcepts().isBlank()) continue;
            for (String c : e.getMissingConcepts().split(",")) {
                String key = c.trim().toLowerCase();
                if (key.isEmpty()) continue;
                missingFrequency.merge(key, 1L, Long::sum);
            }
        }
        List<String> weakConcepts = missingFrequency.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5).map(Map.Entry::getKey).toList();

        Map<String, int[]> conceptCounts = new HashMap<>();
        evaluations.forEach(evaluation -> {
            splitConcepts(evaluation.getMatchedConcepts()).forEach(concept -> conceptCounts.computeIfAbsent(concept, ignored -> new int[2])[0]++);
            splitConcepts(evaluation.getMissingConcepts()).forEach(concept -> conceptCounts.computeIfAbsent(concept, ignored -> new int[2])[1]++);
        });
        Map<String, Double> conceptMastery = conceptCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> round2(100.0 * entry.getValue()[0] / Math.max(1, entry.getValue()[0] + entry.getValue()[1])),
                        (left, right) -> left, LinkedHashMap::new));
        List<String> strongConcepts = conceptMastery.entrySet().stream()
                .filter(entry -> entry.getValue() >= 60.0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5).map(Map.Entry::getKey).toList();
        List<LearningRecommendation> recommendations = weakConcepts.stream()
                .map(concept -> {
                    double mastery = conceptMastery.getOrDefault(concept, 0.0);
                    long attempts = conceptCounts.getOrDefault(concept, new int[2])[0]
                            + conceptCounts.getOrDefault(concept, new int[2])[1];
                    String priority = mastery < 40 ? "CRITICAL" : mastery < 60 ? "HIGH" : "MEDIUM";
                    String reason = "Recommended because concept coverage for " + concept
                            + " was missing in " + attempts + " evaluated occurrence(s).";
                    return new LearningRecommendation(
                            concept, priority, mastery, attempts, reason,
                            List.of("Review the concept", "Practice related questions", "Re-test after improvement"));
                }).toList();

        List<StudentAnalyticsResponse.ProgressPoint> progress = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getAnswerSheet().getExam()))
                .entrySet().stream()
                .map(entry -> new StudentAnalyticsResponse.ProgressPoint(
                        entry.getKey().getTitle(),
                        entry.getKey().getCreatedAt(),
                        round2(entry.getValue().stream()
                                .mapToDouble(this::scorePercent)
                                .average().orElse(0.0))
                ))
                .sorted(Comparator.comparing(StudentAnalyticsResponse.ProgressPoint::date))
                .toList();

        return new StudentAnalyticsResponse(
                                studentId, (long) sheets.size(), round2(avgPercent), round2(highestPercent),
                                round2(recentPercent), improvementPercent, subjectAvg, weakConcepts,
                                strongConcepts, conceptMastery, recommendations, progress
        );
    }

        private double scorePercent(Evaluation evaluation) {
                double marks = evaluation.getFinalMarks() != null ? evaluation.getFinalMarks() : evaluation.getMarksAwarded();
                return safeDiv(marks, evaluation.getMaxMarks()) * 100.0;
        }

        private List<String> splitConcepts(String concepts) {
                if (concepts == null || concepts.isBlank()) return List.of();
                return Arrays.stream(concepts.split(",")).map(String::trim)
                                .map(String::toLowerCase).filter(value -> !value.isBlank()).toList();
        }

        public ClassAnalyticsResponse classAnalytics(Long examId, UserPrincipal principal) {
                Exam exam = examService.findOrThrow(examId);
                authorizationService.requireExamWrite(exam, principal);
        List<AnswerSheet> sheets = answerSheetRepository.findByExamId(examId);
        List<Evaluation> evaluations = evaluationRepository.findByAnswerSheet_Exam_Id(examId);

        long evaluatedCount = sheets.stream().filter(s -> s.getStatus() == AnswerSheetStatus.PROCESSED).count();
        long pendingReview = evaluations.stream().filter(e -> e.getStatus() == EvaluationStatus.NEEDS_REVIEW).count();

        // Aggregate percentage-of-max per answer sheet (sum marks / sum maxMarks across its questions)
        Map<Long, List<Evaluation>> bySheet = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getAnswerSheet().getId()));
        List<Double> sheetPercentages = bySheet.values().stream()
                .map(evals -> {
                    double marks = evals.stream().mapToDouble(Evaluation::getMarksAwarded).sum();
                    double max = evals.stream().mapToDouble(Evaluation::getMaxMarks).sum();
                    return safeDiv(marks, max) * 100.0;
                })
                .sorted()
                .toList();

        double mean = sheetPercentages.isEmpty() ? 0.0 : sheetPercentages.stream().mapToDouble(d -> d).average().orElse(0.0);
        double median = median(sheetPercentages);
        double highest = sheetPercentages.isEmpty() ? 0.0 : sheetPercentages.get(sheetPercentages.size() - 1);
        double lowest = sheetPercentages.isEmpty() ? 0.0 : sheetPercentages.get(0);
        double avgConfidence = evaluations.isEmpty() ? 0.0 :
                evaluations.stream().mapToDouble(e -> e.getOverallConfidence() != null ? e.getOverallConfidence() : 0.0).average().orElse(0.0);
        double manualReviewRate = evaluations.isEmpty() ? 0.0 : (100.0 * pendingReview / evaluations.size());

        Map<Long, List<Evaluation>> byQuestion = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getQuestion().getId()));
        List<ClassAnalyticsResponse.QuestionStat> questionStats = byQuestion.entrySet().stream()
                .map(entry -> {
                    List<Evaluation> evals = entry.getValue();
                    Question q = evals.get(0).getQuestion();
                    double avgMarks = evals.stream().mapToDouble(Evaluation::getMarksAwarded).average().orElse(0.0);
                    // difficulty estimate: 1 - (avg score ratio); higher = harder
                    double difficulty = 1.0 - safeDiv(avgMarks, q.getMaxMarks());
                    Map<String, Long> missingFreq = new HashMap<>();
                    for (Evaluation e : evals) {
                        if (e.getMissingConcepts() == null) continue;
                        for (String c : e.getMissingConcepts().split(",")) {
                            String key = c.trim().toLowerCase();
                            if (!key.isEmpty()) missingFreq.merge(key, 1L, Long::sum);
                        }
                    }
                    List<String> commonMissing = missingFreq.entrySet().stream()
                            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                            .limit(3).map(Map.Entry::getKey).toList();
                    double difficultyPercent = difficulty * 100.0;
                    return new ClassAnalyticsResponse.QuestionStat(
                            q.getId(), q.getQuestionNumber(), round2(avgMarks), q.getMaxMarks(), round2(difficulty),
                            (long) evals.size(), classifyDifficulty(evals.size(), difficultyPercent), commonMissing
                    );
                })
                .sorted(Comparator.comparing(ClassAnalyticsResponse.QuestionStat::questionNumber))
                .toList();

        return new ClassAnalyticsResponse(
                examId, (long) sheets.size(), evaluatedCount, pendingReview,
                round2(mean), round2(median), round2(highest), round2(lowest),
                round2(avgConfidence), round2(manualReviewRate), questionStats
        );
    }

    public ExamAnalyticsResponse examAnalytics(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamWrite(exam, principal);
        List<AnswerSheet> sheets = answerSheetRepository.findByExamId(examId);
        List<Evaluation> evaluations = evaluationRepository.findByAnswerSheet_Exam_Id(examId).stream()
                .filter(e -> e.getMaxMarks() != null && e.getMaxMarks() > 0 && e.getMarksAwarded() != null)
                .toList();

        Map<Long, List<Evaluation>> bySheet = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getAnswerSheet().getId()));
        List<Double> studentScores = bySheet.values().stream()
                .map(this::sheetPercent)
                .sorted()
                .toList();
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-20", countRange(studentScores, 0, 20));
        distribution.put("21-40", countRange(studentScores, 21, 40));
        distribution.put("41-60", countRange(studentScores, 41, 60));
        distribution.put("61-80", countRange(studentScores, 61, 80));
        distribution.put("81-100", countRange(studentScores, 81, 100));

        Map<Long, List<Evaluation>> byQuestion = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getQuestion().getId()));
        List<ExamAnalyticsResponse.QuestionAnalytics> questionRows = byQuestion.values().stream()
                .map(this::questionAnalytics)
                .sorted(Comparator.comparing(ExamAnalyticsResponse.QuestionAnalytics::questionNumber))
                .toList();

        Map<String, int[]> conceptCounts = new HashMap<>();
        evaluations.forEach(e -> {
            splitConcepts(e.getMatchedConcepts()).forEach(c -> conceptCounts.computeIfAbsent(c, ignored -> new int[2])[0]++);
            splitConcepts(e.getMissingConcepts()).forEach(c -> conceptCounts.computeIfAbsent(c, ignored -> new int[2])[1]++);
        });
        Map<String, Double> conceptMastery = conceptCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> round2(100.0 * entry.getValue()[0] / Math.max(1, entry.getValue()[0] + entry.getValue()[1])),
                        (left, right) -> left, LinkedHashMap::new));
        List<Map.Entry<String, Double>> orderedConcepts = conceptMastery.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).toList();
        List<String> weakConcepts = orderedConcepts.stream().limit(5).map(Map.Entry::getKey).toList();
        List<String> strongConcepts = orderedConcepts.reversed().stream().limit(5).map(Map.Entry::getKey).toList();

        List<String> insights = new ArrayList<>();
        questionRows.stream().min(Comparator.comparing(ExamAnalyticsResponse.QuestionAnalytics::averagePercent))
                .ifPresent(q -> insights.add("Q" + q.questionNumber() + " has the lowest average performance."));
        orderedConcepts.stream().findFirst().ifPresent(c -> insights.add(c.getKey() + " has the lowest concept mastery."));
        if (!studentScores.isEmpty()) {
            long belowHalf = studentScores.stream().filter(score -> score < 50.0).count();
            insights.add(round2(100.0 * belowHalf / studentScores.size()) + "% of evaluated students scored below 50%.");
        }

        return new ExamAnalyticsResponse(examId, sheets.stream().map(AnswerSheet::getStudent).map(u -> u.getId()).distinct().count(),
                (long) studentScores.size(), (long) evaluations.size(), average(studentScores), median(studentScores),
                studentScores.isEmpty() ? 0.0 : studentScores.get(studentScores.size() - 1),
                studentScores.isEmpty() ? 0.0 : studentScores.get(0), distribution, questionRows,
                conceptMastery, strongConcepts, weakConcepts, insights);
    }

    private ExamAnalyticsResponse.QuestionAnalytics questionAnalytics(List<Evaluation> evaluations) {
        Question question = evaluations.get(0).getQuestion();
        List<Double> scores = evaluations.stream().map(this::scorePercent).sorted().toList();
        double averagePercent = average(scores);
        double coverage = evaluations.stream().mapToDouble(e -> e.getConceptCoverage() == null ? 0.0 : e.getConceptCoverage() * 100.0).average().orElse(0.0);
        long low = scores.stream().filter(score -> score < 40.0).count();
        long high = scores.stream().filter(score -> score >= 80.0).count();
        return new ExamAnalyticsResponse.QuestionAnalytics(question.getId(), question.getQuestionNumber(), question.getText(),
                question.getMaxMarks(), (long) scores.size(), round2(evaluations.stream().mapToDouble(Evaluation::getMarksAwarded).average().orElse(0.0)),
                round2(averagePercent), classifyAverage(scores.size(), averagePercent), round2(coverage),
                round2(100.0 * low / scores.size()), round2(100.0 * high / scores.size()), scores);
    }

    private String classifyAverage(int attempts, double averagePercent) {
        if (attempts < questionDifficultyMinimumSample) return "INSUFFICIENT_DATA";
        if (averagePercent >= 80.0) return "EASY";
        if (averagePercent >= 60.0) return "MEDIUM";
        if (averagePercent >= 40.0) return "HARD";
        return "VERY_HARD";
    }

    private double sheetPercent(List<Evaluation> evaluations) {
        double marks = evaluations.stream().mapToDouble(e -> e.getFinalMarks() != null ? e.getFinalMarks() : e.getMarksAwarded()).sum();
        double max = evaluations.stream().mapToDouble(Evaluation::getMaxMarks).sum();
        return safeDiv(marks, max) * 100.0;
    }

    private long countRange(List<Double> values, int min, int max) {
        return values.stream().filter(value -> value >= min && value <= max).count();
    }

    private double average(List<Double> values) { return values.isEmpty() ? 0.0 : round2(values.stream().mapToDouble(v -> v).average().orElse(0.0)); }

    private double median(List<Double> sorted) {
        if (sorted.isEmpty()) return 0.0;
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

        private String classifyDifficulty(int attempts, double difficultyPercent) {
                if (attempts < questionDifficultyMinimumSample) return "INSUFFICIENT_DATA";
                if (difficultyPercent < 25.0) return "EASY";
                if (difficultyPercent < 50.0) return "MEDIUM";
                if (difficultyPercent < 75.0) return "HARD";
                return "VERY_HARD";
        }

    private double safeDiv(double a, double b) {
        return b == 0 ? 0.0 : a / b;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
