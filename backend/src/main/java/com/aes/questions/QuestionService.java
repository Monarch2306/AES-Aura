package com.aes.questions;

import com.aes.common.BadRequestException;
import com.aes.common.ResourceNotFoundException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.auth.UserPrincipal;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import com.aes.questions.dto.QuestionCreateRequest;
import com.aes.questions.dto.QuestionResponse;
import com.aes.rubrics.RubricRepository;
import com.aes.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@SuppressWarnings("null")
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamService examService;
    private final RubricRepository rubricRepository;
    private final ResourceAuthorizationService authorizationService;
    private final AuditService auditService;

    public QuestionService(QuestionRepository questionRepository, ExamService examService, RubricRepository rubricRepository,
                           ResourceAuthorizationService authorizationService, AuditService auditService) {
        this.questionRepository = questionRepository;
        this.examService = examService;
        this.rubricRepository = rubricRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public QuestionResponse create(QuestionCreateRequest request, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(request.examId());
        authorizationService.requireExamWrite(exam, principal);
        questionRepository.findByExamIdAndQuestionNumber(exam.getId(), request.questionNumber())
                .ifPresent(q -> { throw new BadRequestException(
                        "Question number " + request.questionNumber() + " already exists for this exam"); });

        Question question = new Question();
        question.setExam(exam);
        question.setQuestionNumber(request.questionNumber());
        question.setText(request.text());
        question.setMaxMarks(request.maxMarks());
        question = questionRepository.save(question);
        if (auditService != null) auditService.log(principal.getId(), "QUESTION_CREATED", "Question", question.getId(), "examId=" + exam.getId());
        return toResponse(question);
    }

    public List<QuestionResponse> listByExam(Long examId, UserPrincipal principal) {
        authorizationService.requireExamRead(examService.findOrThrow(examId), principal);
        return questionRepository.findByExamIdOrderByQuestionNumberAsc(examId).stream().map(this::toResponse).toList();
    }

    public Question findOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
    }

    private QuestionResponse toResponse(Question q) {
        boolean hasRubric = rubricRepository.findByQuestionId(q.getId()).isPresent();
        return new QuestionResponse(q.getId(), q.getExam().getId(), q.getQuestionNumber(), q.getText(), q.getMaxMarks(), hasRubric);
    }
}
