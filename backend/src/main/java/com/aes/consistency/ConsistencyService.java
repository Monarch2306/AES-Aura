package com.aes.consistency;

import com.aes.audit.AuditService;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.evaluations.Evaluation;
import com.aes.evaluations.EvaluationRepository;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import com.aes.similarity.AnswerSimilarityCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class ConsistencyService {
    private final EvaluationRepository evaluationRepository;
    private final ConsistencyFlagRepository flagRepository;
    private final ExamService examService;
    private final ResourceAuthorizationService authorizationService;
    private final double minSimilarity;
    private final double maxConceptDifference;
    private final double minMarkDifference;
    private final AuditService auditService;

    public ConsistencyService(EvaluationRepository evaluationRepository, ConsistencyFlagRepository flagRepository,
                              ExamService examService, ResourceAuthorizationService authorizationService,
                              @Value("${app.analytics.consistency-min-similarity:0.85}") double minSimilarity,
                              @Value("${app.analytics.consistency-max-concept-difference:0.10}") double maxConceptDifference,
                            @Value("${app.analytics.consistency-min-mark-difference:2.0}") double minMarkDifference,
                            AuditService auditService) {
        this.evaluationRepository = evaluationRepository;
        this.flagRepository = flagRepository;
        this.examService = examService;
        this.authorizationService = authorizationService;
        this.minSimilarity = minSimilarity;
        this.maxConceptDifference = maxConceptDifference;
        this.minMarkDifference = minMarkDifference;
        this.auditService = auditService;
    }

    @Transactional
    public List<ConsistencyFlagResponse> findFlags(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamWrite(exam, principal);
        Map<Long, List<Evaluation>> byQuestion = evaluationRepository.findByAnswerSheet_Exam_Id(examId).stream()
                .collect(Collectors.groupingBy(e -> e.getQuestion().getId()));
        byQuestion.values().forEach(this::detectPairs);
        return flagRepository.findByExamIdOrderByMarksDifferenceDesc(examId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ConsistencyFlagResponse dismiss(Long id, ConsistencyReviewRequest request, UserPrincipal principal) {
        return review(id, ConsistencyReviewStatus.DISMISSED, request, principal);
    }

    @Transactional
    public ConsistencyFlagResponse resolve(Long id, ConsistencyReviewRequest request, UserPrincipal principal) {
        return review(id, ConsistencyReviewStatus.RESOLVED, request, principal);
    }

    private ConsistencyFlagResponse review(Long id, ConsistencyReviewStatus status, ConsistencyReviewRequest request, UserPrincipal principal) {
        ConsistencyFlag flag = flagRepository.findById(id).orElseThrow();
        authorizationService.requireExamWrite(examService.findOrThrow(flag.getExamId()), principal);
        flag.setStatus(status); flag.setReviewedBy(principal.getId()); flag.setReviewedAt(Instant.now()); flag.setReviewNotes(request.notes());
        auditService.log(principal.getId(), "CONSISTENCY_REVIEWED", "ConsistencyFlag", id, "status=" + status);
        return toResponse(flagRepository.save(flag));
    }

    private void detectPairs(List<Evaluation> evaluations) {
        for (int first = 0; first < evaluations.size(); first++) {
            for (int second = first + 1; second < evaluations.size(); second++) {
                Evaluation left = evaluations.get(first), right = evaluations.get(second);
                if (left.getStudentAnswerText() == null || left.getStudentAnswerText().isBlank()
                        || right.getStudentAnswerText() == null || right.getStudentAnswerText().isBlank()
                        || Objects.equals(left.getAnswerSheet().getStudent().getId(), right.getAnswerSheet().getStudent().getId())) {
                    continue;
                }
                long firstId = Math.min(left.getId(), right.getId()), secondId = Math.max(left.getId(), right.getId());
                if (flagRepository.findByEvaluationIdAndComparedEvaluationId(firstId, secondId).isPresent()) continue;
                double semantic = AnswerSimilarityCalculator.tfIdfCosine(
                    left.getStudentAnswerText(), right.getStudentAnswerText());
                double conceptDifference = Math.abs(value(left.getConceptCoverage()) - value(right.getConceptCoverage()));
                double marksDifference = Math.abs(effectiveMarks(left) - effectiveMarks(right));
                if (semantic < minSimilarity || conceptDifference > maxConceptDifference || marksDifference < minMarkDifference) continue;
                ConsistencyFlag flag = new ConsistencyFlag();
                flag.setExamId(left.getAnswerSheet().getExam().getId()); flag.setQuestionId(left.getQuestion().getId());
                flag.setEvaluationId(firstId); flag.setComparedEvaluationId(secondId);
                flag.setSemanticSimilarity(semantic); flag.setConceptCoverageDifference(conceptDifference); flag.setMarksDifference(marksDifference);
                flag.setSeverity(marksDifference >= minMarkDifference * 2 ? "HIGH" : "MEDIUM");
                flag.setReason("Answers have similar semantic signals and concept coverage but substantially different awarded marks.");
                flag.setStatus(ConsistencyReviewStatus.REVIEW_REQUIRED);
                flagRepository.save(flag);
            }
        }
    }

    private ConsistencyFlagResponse toResponse(ConsistencyFlag flag) {
        Evaluation evaluation = evaluationRepository.findById(flag.getEvaluationId()).orElse(null);
        Evaluation compared = evaluationRepository.findById(flag.getComparedEvaluationId()).orElse(null);
        return new ConsistencyFlagResponse(flag.getId(), flag.getQuestionId(), evaluation == null ? null : evaluation.getQuestion().getQuestionNumber(),
            flag.getEvaluationId(), flag.getComparedEvaluationId(),
            evaluation == null ? null : evaluation.getStudentAnswerText(),
            compared == null ? null : compared.getStudentAnswerText(),
            evaluation == null ? null : effectiveMarks(evaluation),
            compared == null ? null : effectiveMarks(compared),
            flag.getSemanticSimilarity(), flag.getConceptCoverageDifference(),
                flag.getMarksDifference(), flag.getSeverity(), flag.getReason(), flag.getStatus().name(), flag.getReviewNotes());
    }

    private double effectiveMarks(Evaluation evaluation) { return evaluation.getFinalMarks() != null ? evaluation.getFinalMarks() : value(evaluation.getMarksAwarded()); }
    private double value(Double value) { return value == null ? 0.0 : value; }
}
