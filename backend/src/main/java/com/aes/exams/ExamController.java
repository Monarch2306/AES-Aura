package com.aes.exams;

import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import com.aes.exams.dto.ExamCreateRequest;
import com.aes.exams.dto.ExamResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamResponse> create(@Valid @RequestBody ExamCreateRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(examService.create(request, principal), "Exam created");
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(examService.listAll(principal));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamResponse> get(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(examService.getById(id, principal));
    }
}
