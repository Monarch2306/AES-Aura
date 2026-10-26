package com.aes.exams;

import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceNotFoundException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.audit.AuditService;
import com.aes.exams.dto.ExamCreateRequest;
import com.aes.exams.dto.ExamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@SuppressWarnings("null")
public class ExamService {

    private final ExamRepository examRepository;
    private final ResourceAuthorizationService authorizationService;
    private final AuditService auditService;

    public ExamService(ExamRepository examRepository, ResourceAuthorizationService authorizationService, AuditService auditService) {
        this.examRepository = examRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public ExamResponse create(ExamCreateRequest request, UserPrincipal teacher) {
        Exam exam = new Exam();
        exam.setTitle(request.title());
        exam.setSubject(request.subject());
        exam.setExamDate(request.examDate());
        exam.setTeacher(teacher.getUser());
        exam = examRepository.save(exam);
        if (auditService != null) auditService.log(teacher.getId(), "EXAM_CREATED", "Exam", exam.getId(), "subject=" + exam.getSubject());
        return toResponse(exam);
    }

    public ExamResponse getById(Long id, UserPrincipal principal) {
        Exam exam = findOrThrow(id);
        authorizationService.requireExamRead(exam, principal);
        return toResponse(exam);
    }

    public List<ExamResponse> listAll(UserPrincipal principal) {
        return examRepository.findAll().stream()
                .filter(exam -> authorizationService.isAdmin(principal)
                        || !authorizationService.isTeacher(principal)
                        || exam.getTeacher().getId().equals(principal.getId()))
                .map(this::toResponse).toList();
    }

    public Exam findOrThrow(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + id));
    }

    private ExamResponse toResponse(Exam exam) {
        return new ExamResponse(
                exam.getId(), exam.getTitle(), exam.getSubject(), exam.getExamDate(),
                exam.getTeacher().getId(), exam.getTeacher().getName(), exam.getCreatedAt()
        );
    }
}
