package com.aes.answersheets;

import com.aes.answersheets.dto.AnswerSheetResponse;
import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/answer-sheets")
@SuppressWarnings("null")
public class AnswerSheetController {

    private final AnswerSheetService answerSheetService;

    public AnswerSheetController(AnswerSheetService answerSheetService) {
        this.answerSheetService = answerSheetService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnswerSheetResponse> upload(
            @RequestParam Long examId,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(answerSheetService.upload(examId, file, principal), "Answer sheet uploaded");
    }

    @GetMapping("/{id}")
    public ApiResponse<AnswerSheetResponse> get(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(answerSheetService.getById(id, principal));
    }

    @GetMapping("/{id}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getFile(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AnswerSheetResponse meta = answerSheetService.getById(id, principal);
        org.springframework.core.io.Resource resource = answerSheetService.loadFile(id, principal);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(meta.mimeType()))
                .body(resource);
    }

    @GetMapping("/{id}/pages")
    public ApiResponse<List<com.aes.answersheets.dto.AnswerPageResponse>> listPages(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(answerSheetService.listPages(id, principal));
    }

    @GetMapping("/{id}/pages/{pageNumber}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getPageFile(
            @PathVariable Long id, @PathVariable Integer pageNumber, @AuthenticationPrincipal UserPrincipal principal) {
        AnswerSheetResponse meta = answerSheetService.getById(id, principal);
        org.springframework.core.io.Resource resource = answerSheetService.loadPageFile(id, pageNumber, principal);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(meta.mimeType()))
                .body(resource);
    }

    @GetMapping
    public ApiResponse<List<AnswerSheetResponse>> list(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (examId != null) return ApiResponse.ok(answerSheetService.listByExam(examId, principal));
        if (studentId != null) return ApiResponse.ok(answerSheetService.listByStudent(studentId, principal));
        return ApiResponse.ok(List.of());
    }
}
