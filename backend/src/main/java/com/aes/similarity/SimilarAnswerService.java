package com.aes.similarity;

import com.aes.audit.AuditService;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.evaluations.Evaluation;
import com.aes.evaluations.EvaluationRepository;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class SimilarAnswerService {
    private static final String NOTICE = "Similarity is an indicator for manual review, not proof of plagiarism.";
    private final EvaluationRepository evaluationRepository;
    private final SimilarAnswerResultRepository resultRepository;
    private final ExamService examService;
    private final ResourceAuthorizationService authorizationService;
    private final double reviewThreshold;
    private final double textWeight;
    private final AuditService auditService;

    public SimilarAnswerService(EvaluationRepository evaluationRepository, SimilarAnswerResultRepository resultRepository,
                                ExamService examService, ResourceAuthorizationService authorizationService,
                                double reviewThreshold) {
                                this(evaluationRepository, resultRepository, examService, authorizationService, reviewThreshold, 0.7, null);
    }

    @Autowired
    public SimilarAnswerService(EvaluationRepository evaluationRepository, SimilarAnswerResultRepository resultRepository,
                                ExamService examService, ResourceAuthorizationService authorizationService,
                                @Value("${app.analytics.similarity-review-threshold:0.85}") double reviewThreshold,
                                @Value("${app.analytics.similarity-text-weight:0.70}") double textWeight,
                                AuditService auditService) {
        this.evaluationRepository = evaluationRepository;
        this.resultRepository = resultRepository;
        this.examService = examService;
        this.authorizationService = authorizationService;
        this.reviewThreshold = reviewThreshold;
        this.textWeight = Math.max(0.0, Math.min(1.0, textWeight));
        this.auditService = auditService;
    }

    @Transactional
    public List<SimilarAnswerResponse> findSimilarAnswers(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamWrite(exam, principal);
        Map<Long, List<Evaluation>> byQuestion = evaluationRepository.findByAnswerSheet_Exam_Id(examId).stream()
                .filter(e -> e.getStudentAnswerText() != null && !e.getStudentAnswerText().isBlank())
                .collect(Collectors.groupingBy(e -> e.getQuestion().getId()));
        byQuestion.values().forEach(this::detectPairs);
        return resultRepository.findByExamIdOrderByCombinedSimilarityDesc(examId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SimilarAnswerResponse review(Long id, SimilarityReviewStatus status, SimilarityReviewRequest request, UserPrincipal principal) {
        SimilarAnswerResult result = resultRepository.findById(id).orElseThrow();
        Exam exam = examService.findOrThrow(result.getExamId());
        authorizationService.requireExamWrite(exam, principal);
        if (status != SimilarityReviewStatus.DISMISSED && status != SimilarityReviewStatus.CONFIRMED_BY_REVIEWER) {
            throw new IllegalArgumentException("Only DISMISSED or CONFIRMED_BY_REVIEWER are valid review actions");
        }
        result.setStatus(status);
        result.setReviewedBy(principal.getId());
        result.setReviewedAt(Instant.now());
        result.setReviewNotes(request.notes());
        if (auditService != null) {
            auditService.log(principal.getId(), "SIMILARITY_REVIEWED", "SimilarAnswerResult", id, "status=" + status);
        }
        return toResponse(resultRepository.save(result));
    }

    private void detectPairs(List<Evaluation> evaluations) {
        for (int first = 0; first < evaluations.size(); first++) {
            for (int second = first + 1; second < evaluations.size(); second++) {
                Evaluation left = evaluations.get(first);
                Evaluation right = evaluations.get(second);
                if (Objects.equals(left.getAnswerSheet().getStudent().getId(), right.getAnswerSheet().getStudent().getId())) continue;
                long firstId = Math.min(left.getId(), right.getId());
                long secondId = Math.max(left.getId(), right.getId());
                if (resultRepository.findByFirstEvaluationIdAndSecondEvaluationId(firstId, secondId).isPresent()) continue;
                double text = AnswerSimilarityCalculator.tfIdfCosine(left.getStudentAnswerText(), right.getStudentAnswerText());
                double concepts = AnswerSimilarityCalculator.conceptOverlap(left.getMatchedConcepts(), right.getMatchedConcepts());
                double combined = AnswerSimilarityCalculator.combined(text, concepts, textWeight);
                if (combined < reviewThreshold) continue;
                Evaluation canonicalLeft = left.getId().equals(firstId) ? left : right;
                Evaluation canonicalRight = left.getId().equals(firstId) ? right : left;
                SimilarAnswerResult result = new SimilarAnswerResult();
                result.setExamId(left.getAnswerSheet().getExam().getId());
                result.setQuestionId(left.getQuestion().getId());
                result.setFirstEvaluationId(firstId); result.setFirstStudentId(canonicalLeft.getAnswerSheet().getStudent().getId());
                result.setSecondEvaluationId(secondId); result.setSecondStudentId(canonicalRight.getAnswerSheet().getStudent().getId());
                result.setTextSimilarity(text); result.setConceptSimilarity(concepts); result.setCombinedSimilarity(combined);
                result.setSeverity(combined >= 0.9 ? "HIGH" : combined >= 0.75 ? "MEDIUM" : "LOW");
                result.setStatus(SimilarityReviewStatus.REVIEW_REQUIRED);
                resultRepository.save(result);
            }
        }
    }

    private SimilarAnswerResponse toResponse(SimilarAnswerResult result) {
        Evaluation first = evaluationRepository.findById(result.getFirstEvaluationId()).orElse(null);
        Evaluation second = evaluationRepository.findById(result.getSecondEvaluationId()).orElse(null);
        return new SimilarAnswerResponse(result.getId(), result.getQuestionId(),
                first == null ? null : first.getQuestion().getQuestionNumber(), result.getFirstEvaluationId(), result.getFirstStudentId(),
                first == null ? null : first.getStudentAnswerText(), result.getSecondEvaluationId(), result.getSecondStudentId(),
                second == null ? null : second.getStudentAnswerText(), result.getTextSimilarity(), result.getConceptSimilarity(),
                result.getCombinedSimilarity(), result.getSeverity(), result.getStatus().name(), NOTICE,
                first == null ? null : first.getMatchedConcepts(), second == null ? null : second.getMatchedConcepts(), result.getReviewNotes());
    }

}
