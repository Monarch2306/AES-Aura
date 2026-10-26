package com.aes.questions;

import com.aes.common.ApiResponse;
import com.aes.auth.UserPrincipal;
import com.aes.questions.dto.QuestionCreateRequest;
import com.aes.questions.dto.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuestionResponse> create(@Valid @RequestBody QuestionCreateRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionService.create(request, principal), "Question created");
    }

    @GetMapping
    public ApiResponse<List<QuestionResponse>> listByExam(@RequestParam Long examId,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionService.listByExam(examId, principal));
    }
}
