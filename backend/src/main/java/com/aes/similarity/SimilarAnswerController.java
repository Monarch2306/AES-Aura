package com.aes.similarity;

import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class SimilarAnswerController {
    private final SimilarAnswerService service;

    public SimilarAnswerController(SimilarAnswerService service) {
        this.service = service;
    }

    @GetMapping("/{examId}/similar-answers")
    public ApiResponse<List<SimilarAnswerResponse>> similarAnswers(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(service.findSimilarAnswers(examId, principal));
    }

    @PostMapping("/similar-answers/{id}/dismiss")
    public ApiResponse<SimilarAnswerResponse> dismiss(@PathVariable Long id, @Valid @RequestBody SimilarityReviewRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.review(id, SimilarityReviewStatus.DISMISSED, request, principal));
    }

    @PostMapping("/similar-answers/{id}/confirm")
    public ApiResponse<SimilarAnswerResponse> confirm(@PathVariable Long id, @Valid @RequestBody SimilarityReviewRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.review(id, SimilarityReviewStatus.CONFIRMED_BY_REVIEWER, request, principal));
    }
}
