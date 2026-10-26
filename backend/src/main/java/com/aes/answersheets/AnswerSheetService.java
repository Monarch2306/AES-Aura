package com.aes.answersheets;

import com.aes.answersheets.dto.AnswerPageResponse;
import com.aes.answersheets.dto.AnswerSheetResponse;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceNotFoundException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.audit.AuditService;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Arrays;

@Service
@SuppressWarnings("null")
public class AnswerSheetService {

    private final AnswerSheetRepository repository;
    private final ExamService examService;
    private final FileStorageService fileStorageService;
    private final ResourceAuthorizationService authorizationService;
    private final AuditService auditService;
    private final AnswerPageRepository answerPageRepository;

    @Autowired
    public AnswerSheetService(AnswerSheetRepository repository, ExamService examService, FileStorageService fileStorageService,
                              ResourceAuthorizationService authorizationService, AuditService auditService,
                              AnswerPageRepository answerPageRepository) {
        this.repository = repository;
        this.examService = examService;
        this.fileStorageService = fileStorageService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.answerPageRepository = answerPageRepository;
    }

    public AnswerSheetService(AnswerSheetRepository repository, ExamService examService, FileStorageService fileStorageService,
                              ResourceAuthorizationService authorizationService, AuditService auditService) {
        this(repository, examService, fileStorageService, authorizationService, auditService, null);
    }

    @Transactional
    public AnswerSheetResponse upload(Long examId, MultipartFile file, UserPrincipal student) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamWrite(exam, student);
        FileStorageService.StoredFile stored = fileStorageService.store(file);

        AnswerSheet sheet = new AnswerSheet();
        sheet.setExam(exam);
        sheet.setStudent(student.getUser());
        sheet.setOriginalFileName(stored.originalDisplayName());
        sheet.setStoredFilePath(stored.absolutePath());
        sheet.setMimeType(stored.mimeType());
        sheet.setFileSizeBytes(stored.size());
        sheet.setStatus(AnswerSheetStatus.UPLOADED);
        sheet = repository.save(sheet);
        auditService.log(student.getId(), "ANSWER_SHEET_UPLOADED", "AnswerSheet", sheet.getId(),
            "examId=" + examId + ", status=" + sheet.getStatus());
        return toResponse(sheet);
    }

    public AnswerSheetResponse getById(Long id, UserPrincipal principal) {
        AnswerSheet sheet = findOrThrow(id);
        authorizationService.requireAnswerSheetRead(sheet, principal);
        return toResponse(sheet);
    }

    public List<AnswerSheetResponse> listByExam(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamRead(exam, principal);
        return repository.findByExamId(examId).stream()
                .filter(sheet -> authorizationService.isAdmin(principal)
                        || authorizationService.isTeacher(principal)
                        || sheet.getStudent().getId().equals(principal.getId()))
                .map(this::toResponse).toList();
    }

    public List<AnswerSheet> listEntitiesByExam(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        authorizationService.requireExamRead(exam, principal);
        return repository.findByExamId(examId).stream()
                .filter(sheet -> authorizationService.isAdmin(principal)
                        || authorizationService.isTeacher(principal)
                        || sheet.getStudent().getId().equals(principal.getId()))
                .toList();
    }

    public List<AnswerSheetResponse> listByStudent(Long studentId, UserPrincipal principal) {
        authorizationService.requireStudentSelfOrAdmin(studentId, principal);
        return repository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public AnswerSheet findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Answer sheet not found: " + id));
    }

    public org.springframework.core.io.Resource loadFile(Long id, UserPrincipal principal) {
        AnswerSheet sheet = findOrThrow(id);
        authorizationService.requireAnswerSheetRead(sheet, principal);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(sheet.getStoredFilePath());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Stored file is missing on disk for answer sheet " + id);
        }
        return resource;
    }

    public List<AnswerPageResponse> listPages(Long sheetId, UserPrincipal principal) {
        AnswerSheet sheet = findOrThrow(sheetId);
        authorizationService.requireAnswerSheetRead(sheet, principal);
        if (answerPageRepository == null) return List.of();
        return answerPageRepository.findByAnswerSheetIdOrderByPageNumberAsc(sheetId).stream()
                .map(p -> new AnswerPageResponse(p.getId(), sheetId, p.getPageNumber(), p.getRawOcrText(), p.getOcrConfidence(), p.getCreatedAt()))
                .toList();
    }

    public org.springframework.core.io.Resource loadPageFile(Long sheetId, Integer pageNumber, UserPrincipal principal) {
        AnswerSheet sheet = findOrThrow(sheetId);
        authorizationService.requireAnswerSheetRead(sheet, principal);
        if (answerPageRepository == null) {
            return loadFile(sheetId, principal);
        }
        AnswerPage page = answerPageRepository.findByAnswerSheetIdAndPageNumber(sheetId, pageNumber)
                .orElse(null);
        if (page == null) {
            return loadFile(sheetId, principal);
        }
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(page.getFilePath());
        if (!resource.exists()) {
            return loadFile(sheetId, principal);
        }
        return resource;
    }

    @Transactional
    public void save(AnswerSheet sheet) {
        repository.save(sheet);
    }

    private AnswerSheetResponse toResponse(AnswerSheet s) {
        return new AnswerSheetResponse(
                s.getId(), s.getExam().getId(), s.getStudent().getId(), s.getStudent().getName(),
                s.getOriginalFileName(), s.getMimeType(), s.getFileSizeBytes(), s.getStatus(),
                s.getRawOcrText(), s.getOcrConfidence(), s.getProcessingError(), s.getUploadedAt(), s.getProcessedAt(),
                s.getImageQualityScore(), s.getImageBlurScore(), s.getImageBrightnessScore(),
                s.getImageContrastScore(), s.getImageResolutionWidth(), s.getImageResolutionHeight(),
                s.getImageRotationDetected(), splitWarnings(s.getImageQualityWarnings())
        );
    }

    private List<String> splitWarnings(String warnings) {
        if (warnings == null || warnings.isBlank()) return List.of();
        return Arrays.stream(warnings.split("\\n")).filter(value -> !value.isBlank()).toList();
    }
}
