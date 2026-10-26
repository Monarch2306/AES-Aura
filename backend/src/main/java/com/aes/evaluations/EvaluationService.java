package com.aes.evaluations;

import com.aes.aiclient.AiServiceClient;
import com.aes.aiclient.dto.*;
import com.aes.answersheets.AnswerSheet;
import com.aes.answersheets.AnswerSheetService;
import com.aes.answersheets.AnswerSheetStatus;
import com.aes.audit.AuditService;
import com.aes.auth.UserPrincipal;
import com.aes.common.AiServiceException;
import com.aes.common.BadRequestException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.common.ResourceNotFoundException;
import com.aes.evaluations.dto.EvaluationResponse;
import com.aes.evaluations.dto.EvaluationProgressEvent;
import com.aes.evaluations.dto.CopilotResponse;
import com.aes.evaluations.dto.OverrideRequest;
import com.aes.evaluations.dto.ReviewRequest;
import com.aes.questions.Question;
import com.aes.questions.QuestionRepository;
import com.aes.rubrics.Rubric;
import com.aes.rubrics.RubricRepository;
import com.aes.similarity.SimilarAnswerService;
import com.aes.consistency.ConsistencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationRepository evaluationRepository;
    private final AnswerSheetService answerSheetService;
    private final QuestionRepository questionRepository;
    private final RubricRepository rubricRepository;
    private final AiServiceClient aiServiceClient;
    private final AuditService auditService;
    private final ResourceAuthorizationService authorizationService;
    private final SimilarAnswerService similarAnswerService;
    private final ConsistencyService consistencyService;
    private final EvaluationProgressBroadcaster broadcaster;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EvaluationService(
            EvaluationRepository evaluationRepository, AnswerSheetService answerSheetService,
            QuestionRepository questionRepository, RubricRepository rubricRepository,
            AiServiceClient aiServiceClient, AuditService auditService,
            ResourceAuthorizationService authorizationService, SimilarAnswerService similarAnswerService,
            ConsistencyService consistencyService, EvaluationProgressBroadcaster broadcaster
    ) {
        this.evaluationRepository = evaluationRepository;
        this.answerSheetService = answerSheetService;
        this.questionRepository = questionRepository;
        this.rubricRepository = rubricRepository;
        this.aiServiceClient = aiServiceClient;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
        this.similarAnswerService = similarAnswerService;
        this.consistencyService = consistencyService;
        this.broadcaster = broadcaster;
    }

    public EvaluationService(
            EvaluationRepository evaluationRepository, AnswerSheetService answerSheetService,
            QuestionRepository questionRepository, RubricRepository rubricRepository,
            AiServiceClient aiServiceClient, AuditService auditService,
            ResourceAuthorizationService authorizationService, SimilarAnswerService similarAnswerService,
            ConsistencyService consistencyService
    ) {
        this(evaluationRepository, answerSheetService, questionRepository, rubricRepository,
                aiServiceClient, auditService, authorizationService, similarAnswerService,
                consistencyService, null);
    }

    public List<EvaluationResponse> triggerEvaluation(Long answerSheetId, UserPrincipal actor) {
        AnswerSheet sheet = answerSheetService.findOrThrow(answerSheetId);
        authorizationService.requireEvaluationWrite(sheet, actor);
        sheet.setStatus(AnswerSheetStatus.PROCESSING);
        answerSheetService.save(sheet);
        publishProgress(answerSheetId, "INITIALIZATION", 0, 10, "Initializing evaluation and reading answer sheet...");

        List<Question> examQuestions = questionRepository.findByExamIdOrderByQuestionNumberAsc(sheet.getExam().getId());

        String expectedQuestionsJson;
        try {
            expectedQuestionsJson = objectMapper.writeValueAsString(examQuestions.stream().map(Question::getQuestionNumber).toList());
        } catch (Exception e) {
            expectedQuestionsJson = null;
        }

        publishProgress(answerSheetId, "OCR_PREPROCESSING", 1, 30, "Running OpenCV preprocessing and Tesseract OCR segmentation...");
        ProcessSheetResponseDto processed;
        try {
            processed = aiServiceClient.processAnswerSheet(
                    new FileSystemResource(sheet.getStoredFilePath()), expectedQuestionsJson);
        } catch (AiServiceException e) {
            sheet.setStatus(AnswerSheetStatus.FAILED);
            sheet.setProcessingError(e.getMessage());
            answerSheetService.save(sheet);
            publishProgress(answerSheetId, "FAILED", 0, 0, "AI Service error: " + safeErrorMessage(e));
            log.error("Evaluation aborted: AI service unavailable for answerSheetId={}", answerSheetId, e);
            throw e;
        }

        sheet.setRawOcrText(processed.ocr().text());
        sheet.setOcrConfidence(processed.ocr().confidence());
        if (processed.imageQuality() != null) {
            sheet.setImageQualityScore(processed.imageQuality().qualityScore());
            sheet.setImageBlurScore(processed.imageQuality().blurScore());
            sheet.setImageBrightnessScore(processed.imageQuality().brightnessScore());
            sheet.setImageContrastScore(processed.imageQuality().contrastScore());
            sheet.setImageResolutionWidth(processed.imageQuality().resolutionWidth());
            sheet.setImageResolutionHeight(processed.imageQuality().resolutionHeight());
            sheet.setImageRotationDetected(processed.imageQuality().rotationDetected());
            sheet.setImageQualityWarnings(processed.imageQuality().warnings() == null
                ? null : String.join("\n", processed.imageQuality().warnings()));
        }

        publishProgress(answerSheetId, "RUBRIC_SCORING", 4, 65, "Scoring segmented answers against rubrics and concept models...");

        try {
            List<EvaluationResponse> results = processed.segmentation().answers().stream()
                    .map(segmented -> evaluateOneAnswer(sheet, examQuestions, segmented))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            sheet.setStatus(AnswerSheetStatus.PROCESSED);
            sheet.setProcessedAt(Instant.now());
            sheet.setProcessingError(null);
            answerSheetService.save(sheet);

            publishProgress(answerSheetId, "ANALYSIS", 5, 85, "Checking grading consistency and cross-student similarity...");

            try {
                similarAnswerService.findSimilarAnswers(sheet.getExam().getId(), actor);
                consistencyService.findFlags(sheet.getExam().getId(), actor);
            } catch (RuntimeException analysisFailure) {
                log.warn("Review analysis failed after answerSheetId={}: {}", answerSheetId,
                        safeErrorMessage(analysisFailure));
            }

            auditService.log(actor.getId(), "TRIGGER_EVALUATION", "AnswerSheet", answerSheetId,
                    "Evaluated " + results.size() + " answers");
            publishProgress(answerSheetId, "COMPLETED", 6, 100, "Evaluation complete. Ready for teacher review.");
            return results;
        } catch (RuntimeException e) {
            sheet.setStatus(AnswerSheetStatus.FAILED);
            sheet.setProcessingError("Evaluation failed: " + safeErrorMessage(e));
            answerSheetService.save(sheet);
            publishProgress(answerSheetId, "FAILED", 0, 0, "Evaluation failed: " + safeErrorMessage(e));
            log.error("Evaluation failed for answerSheetId={}", answerSheetId, e);
            throw e;
        }
    }

    private void publishProgress(Long sheetId, String stage, int stepIndex, int progressPercent, String message) {
        if (broadcaster != null && sheetId != null) {
            try {
                broadcaster.broadcast(sheetId, EvaluationProgressEvent.of(sheetId, stage, stepIndex, progressPercent, message));
            } catch (Exception e) {
                log.debug("Failed publishing WebSocket progress event: {}", e.getMessage());
            }
        }
    }

    public List<EvaluationResponse> triggerBatchEvaluation(Long examId, UserPrincipal actor) {
        List<AnswerSheet> sheets = answerSheetService.listEntitiesByExam(examId, actor);
        List<EvaluationResponse> allEvaluations = new java.util.ArrayList<>();
        int successCount = 0;
        for (AnswerSheet sheet : sheets) {
            try {
                List<EvaluationResponse> evals = triggerEvaluation(sheet.getId(), actor);
                allEvaluations.addAll(evals);
                successCount++;
            } catch (Exception e) {
                log.error("Batch evaluation error on answerSheetId={}: {}", sheet.getId(), e.getMessage());
            }
        }
        auditService.log(actor.getId(), "TRIGGER_BATCH_EVALUATION", "Exam", examId,
                "Batch evaluated " + successCount + "/" + sheets.size() + " answer sheets");
        return allEvaluations;
    }

    private String safeErrorMessage(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) return t.getClass().getSimpleName();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private Optional<EvaluationResponse> evaluateOneAnswer(AnswerSheet sheet, List<Question> examQuestions, SegmentedAnswerDto segmented) {
        Optional<Question> questionOpt = examQuestions.stream()
                .filter(q -> q.getQuestionNumber().equals(segmented.questionNumber()))
                .findFirst();
        if (questionOpt.isEmpty()) {
            log.warn("Segmented answer for unknown question number {} on answerSheetId={}",
                    segmented.questionNumber(), sheet.getId());
            return Optional.empty();
        }
        Question question = questionOpt.get();
        Optional<Rubric> rubricOpt = rubricRepository.findByQuestionId(question.getId());
        if (rubricOpt.isEmpty()) {
            log.warn("No rubric defined for questionId={}, skipping AI scoring (needs manual grading)", question.getId());
            return Optional.empty();
        }
        Rubric rubric = rubricOpt.get();

        RubricDto rubricDto = new RubricDto(
                question.getQuestionNumber(), question.getMaxMarks(), rubric.getReferenceAnswer(),
                rubric.getConcepts().stream()
                        .map(c -> new RubricConceptDto(c.getConcept(), c.getWeight(), c.isRequired(), c.isNegative()))
                        .toList(),
                rubric.getMinRequiredConcepts(), rubric.getWeightSemantic(), rubric.getWeightConceptCoverage()
        );

        EvaluateAnswerRequestDto request = new EvaluateAnswerRequestDto(
                segmented.studentAnswer(), segmented.ocrConfidence(), rubricDto, segmented.ocrConfidence()
        );

        EvaluationResultDto result = aiServiceClient.evaluateAnswer(request);

        Evaluation evaluation = evaluationRepository.findByAnswerSheetIdAndQuestionId(sheet.getId(), question.getId())
                .orElseGet(Evaluation::new);
        evaluation.setAnswerSheet(sheet);
        evaluation.setQuestion(question);
        evaluation.setStudentAnswerText(segmented.studentAnswer());
        evaluation.setMarksAwarded(result.marksAwarded());
        evaluation.setMaxMarks(result.maxMarks());
        evaluation.setNormalizedScore(result.normalizedScore());
        evaluation.setTfidfSimilarity(result.tfidfSimilarity());
        evaluation.setSemanticSimilarity(result.semanticSimilarity());
        evaluation.setConceptCoverage(result.conceptCoverage());
        evaluation.setMatchedConcepts(String.join(", ", result.matchedConcepts()));
        evaluation.setMissingConcepts(String.join(", ", result.missingConcepts()));
        evaluation.setContradictedConcepts(String.join(", ", result.contradictedConcepts()));
        evaluation.setOcrConfidence(result.ocrConfidence());
        evaluation.setSegmentationConfidence(result.segmentationConfidence());
        evaluation.setOverallConfidence(result.overallConfidence());
        evaluation.setConfidenceLevel(ConfidenceLevel.valueOf(result.confidenceLevel()));
        evaluation.setStatus(EvaluationStatus.valueOf(result.status()));
        evaluation.setFeedback(result.feedback());
        evaluation.setOriginalAiMarks(result.marksAwarded());
        try {
            evaluation.setExplanationJson(objectMapper.writeValueAsString(result.explanation()));
        } catch (Exception e) {
            evaluation.setExplanationJson(null);
        }
        evaluation.setEvaluatedAt(Instant.now());

        evaluation = evaluationRepository.save(evaluation);
        return Optional.of(toResponse(evaluation));
    }

    public EvaluationResponse getById(Long id, UserPrincipal principal) {
        Evaluation evaluation = findOrThrow(id);
        authorizationService.requireEvaluationRead(evaluation, principal);
        return toResponse(evaluation);
    }

        public CopilotResponse getCopilot(Long evaluationId, UserPrincipal principal) {
        Evaluation evaluation = findOrThrow(evaluationId);
        authorizationService.requireEvaluationRead(evaluation, principal);
        EvaluationResponse response = toResponse(evaluation);
        List<String> strengths = response.matchedConcepts();
        List<String> weaknesses = response.missingConcepts().stream()
            .map(concept -> "Missing concept: " + concept).toList();
        if (!response.contradictedConcepts().isEmpty()) {
            weaknesses = new java.util.ArrayList<>(weaknesses);
            weaknesses.add("Potential contradiction detected");
        }
        String recommendation = response.confidenceLevel() == ConfidenceLevel.LOW
            ? "LOW_CONFIDENCE"
            : response.status() == EvaluationStatus.NEEDS_REVIEW
                || response.confidenceLevel() == ConfidenceLevel.MEDIUM
                ? "REVIEW"
                : "APPROVE";
        String quality = response.overallConfidence() == null ? "UNKNOWN"
            : response.overallConfidence() >= 0.8 ? "STRONG"
            : response.overallConfidence() >= 0.55 ? "MIXED" : "WEAK";
        String explanation = buildCopilotExplanation(response);
        return new CopilotResponse(
            response.id(), response.finalMarks() != null ? response.finalMarks() : response.marksAwarded(),
            response.maxMarks(), response.overallConfidence(), response.semanticSimilarity(),
            response.conceptCoverage(), response.matchedConcepts(), response.missingConcepts(),
            response.contradictedConcepts(), quality, strengths, weaknesses, response.feedback(),
            recommendation, explanation
        );
        }

        private String buildCopilotExplanation(EvaluationResponse response) {
        String coverage = response.missingConcepts().isEmpty()
            ? "The answer covers all rubric concepts detected by the evaluator."
            : "The answer is missing " + response.missingConcepts().size() + " rubric concept(s).";
        String contradiction = response.contradictedConcepts().isEmpty()
            ? " No contradiction was detected."
            : " A potential contradiction requires teacher review.";
        return coverage + contradiction + " The score remains provisional until teacher review.";
        }

    public List<EvaluationResponse> listByAnswerSheet(Long answerSheetId, UserPrincipal principal) {
        AnswerSheet sheet = answerSheetService.findOrThrow(answerSheetId);
        authorizationService.requireAnswerSheetRead(sheet, principal);
        return evaluationRepository.findByAnswerSheetId(answerSheetId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EvaluationResponse review(Long evaluationId, ReviewRequest request, UserPrincipal reviewer) {
        Evaluation evaluation = findOrThrow(evaluationId);
        authorizationService.requireEvaluationWrite(evaluation, reviewer);
        String action = request.action().toUpperCase();
        if (!action.equals("APPROVE") && !action.equals("REJECT")) {
            throw new BadRequestException("action must be APPROVE or REJECT");
        }
        evaluation.setStatus(EvaluationStatus.REVIEWED);
        evaluation.setReviewedByUserId(reviewer.getId());
        evaluation.setReviewedAt(Instant.now());
        if (action.equals("REJECT")) {
            evaluation.setOverrideReason(request.comment() != null ? request.comment() : "Rejected by teacher");
        }
        evaluation = evaluationRepository.save(evaluation);
        auditService.log(reviewer.getId(), "REVIEW_" + action, "Evaluation", evaluationId, request.comment());
        return toResponse(evaluation);
    }

    @Transactional
    public EvaluationResponse override(Long evaluationId, OverrideRequest request, UserPrincipal reviewer) {
        Evaluation evaluation = findOrThrow(evaluationId);
        authorizationService.requireEvaluationWrite(evaluation, reviewer);
        if (request.finalMarks() > evaluation.getMaxMarks()) {
            throw new BadRequestException("finalMarks cannot exceed the question's maximum marks (" + evaluation.getMaxMarks() + ")");
        }
        evaluation.setOriginalAiMarks(evaluation.getOriginalAiMarks() != null ? evaluation.getOriginalAiMarks() : evaluation.getMarksAwarded());
        evaluation.setFinalMarks(request.finalMarks());
        evaluation.setMarksAwarded(request.finalMarks());
        evaluation.setOverrideReason(request.overrideReason());
        if (request.updatedFeedback() != null && !request.updatedFeedback().isBlank()) {
            evaluation.setFeedback(request.updatedFeedback());
        }
        evaluation.setStatus(EvaluationStatus.REVIEWED);
        evaluation.setReviewedByUserId(reviewer.getId());
        evaluation.setReviewedAt(Instant.now());
        evaluation = evaluationRepository.save(evaluation);
        auditService.log(reviewer.getId(), "OVERRIDE", "Evaluation", evaluationId,
                "originalAiMarks=" + evaluation.getOriginalAiMarks() + " finalMarks=" + request.finalMarks() +
                        " reason=" + request.overrideReason());
        return toResponse(evaluation);
    }

    public Evaluation findOrThrow(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private EvaluationResponse toResponse(Evaluation e) {
        return new EvaluationResponse(
                e.getId(), e.getAnswerSheet().getId(), e.getQuestion().getId(), e.getQuestion().getQuestionNumber(),
                e.getStudentAnswerText(), e.getMarksAwarded(), e.getMaxMarks(), e.getNormalizedScore(),
                e.getTfidfSimilarity(), e.getSemanticSimilarity(), e.getConceptCoverage(),
                splitCsv(e.getMatchedConcepts()), splitCsv(e.getMissingConcepts()), splitCsv(e.getContradictedConcepts()),
                e.getOcrConfidence(), e.getSegmentationConfidence(), e.getOverallConfidence(),
                e.getConfidenceLevel(), e.getStatus(), e.getFeedback(),
                e.getOriginalAiMarks(), e.getFinalMarks(), e.getOverrideReason(),
                e.getReviewedByUserId(), e.getReviewedAt(), e.getEvaluatedAt()
        );
    }
}
