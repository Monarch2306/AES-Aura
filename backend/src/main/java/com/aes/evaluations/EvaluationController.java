package com.aes.evaluations;

import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import com.aes.evaluations.dto.EvaluationResponse;
import com.aes.evaluations.dto.CopilotResponse;
import com.aes.evaluations.dto.OverrideRequest;
import com.aes.evaluations.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/{answerSheetId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<EvaluationResponse>> trigger(@PathVariable Long answerSheetId,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.triggerEvaluation(answerSheetId, principal), "Evaluation completed");
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<EvaluationResponse>> batchTrigger(@RequestParam Long examId,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.triggerBatchEvaluation(examId, principal), "Batch evaluation completed");
    }

    @GetMapping("/{id}")
    public ApiResponse<EvaluationResponse> get(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.getById(id, principal));
    }

    @GetMapping("/{id}/copilot")
    public ApiResponse<CopilotResponse> copilot(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.getCopilot(id, principal));
    }

    @GetMapping
    public ApiResponse<List<EvaluationResponse>> listByAnswerSheet(@RequestParam Long answerSheetId,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.listByAnswerSheet(answerSheetId, principal));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<EvaluationResponse> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.review(id, request, principal), "Review recorded");
    }

    @PostMapping("/{id}/override")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<EvaluationResponse> override(@PathVariable Long id, @Valid @RequestBody OverrideRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(evaluationService.override(id, request, principal), "Marks overridden");
    }
}
