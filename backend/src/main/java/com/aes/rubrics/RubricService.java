package com.aes.rubrics;

import com.aes.common.BadRequestException;
import com.aes.common.ResourceNotFoundException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.auth.UserPrincipal;
import com.aes.questions.Question;
import com.aes.questions.QuestionService;
import com.aes.rubrics.dto.RubricConceptRequest;
import com.aes.rubrics.dto.RubricCreateRequest;
import com.aes.rubrics.dto.RubricResponse;
import com.aes.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RubricService {

    private final RubricRepository rubricRepository;
    private final QuestionService questionService;
    private final ResourceAuthorizationService authorizationService;
    private final AuditService auditService;

    public RubricService(RubricRepository rubricRepository, QuestionService questionService,
                         ResourceAuthorizationService authorizationService, AuditService auditService) {
        this.rubricRepository = rubricRepository;
        this.questionService = questionService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public RubricResponse create(RubricCreateRequest request, UserPrincipal principal) {
        Question question = questionService.findOrThrow(request.questionId());
        authorizationService.requireQuestionWrite(question, principal);
        if (rubricRepository.findByQuestionId(question.getId()).isPresent()) {
            throw new BadRequestException("A rubric already exists for question " + question.getId());
        }

        Rubric rubric = new Rubric();
        rubric.setQuestion(question);
        rubric.setReferenceAnswer(request.referenceAnswer());
        rubric.setMinRequiredConcepts(request.minRequiredConcepts() != null ? request.minRequiredConcepts() : 0);
        rubric.setWeightSemantic(request.weightSemantic());
        rubric.setWeightConceptCoverage(request.weightConceptCoverage());

        for (RubricConceptRequest c : request.concepts()) {
            RubricConcept concept = new RubricConcept();
            concept.setRubric(rubric);
            concept.setConcept(c.concept());
            concept.setWeight(c.weight());
            concept.setRequired(c.required());
            concept.setNegative(c.negative());
            rubric.getConcepts().add(concept);
        }

        rubric = rubricRepository.save(rubric);
        if (auditService != null) auditService.log(principal.getId(), "RUBRIC_CREATED", "Rubric", rubric.getId(), "questionId=" + question.getId());
        return toResponse(rubric);
    }

    public RubricResponse getByQuestionId(Long questionId, UserPrincipal principal) {
        Question question = questionService.findOrThrow(questionId);
        authorizationService.requireRubricRead(question, principal);
        Rubric rubric = rubricRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("No rubric defined for question " + questionId));
        return toResponse(rubric);
    }

    public Rubric findByQuestionIdOrThrow(Long questionId) {
        return rubricRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("No rubric defined for question " + questionId));
    }

    private RubricResponse toResponse(Rubric rubric) {
        return new RubricResponse(
                rubric.getId(), rubric.getQuestion().getId(), rubric.getReferenceAnswer(),
                rubric.getConcepts().stream()
                        .map(c -> new RubricConceptRequest(c.getConcept(), c.getWeight(), c.isRequired(), c.isNegative()))
                        .toList(),
                rubric.getMinRequiredConcepts(), rubric.getWeightSemantic(), rubric.getWeightConceptCoverage()
        );
    }
}
