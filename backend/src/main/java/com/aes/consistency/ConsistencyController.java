package com.aes.consistency;

import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ConsistencyController {
    private final ConsistencyService service;

    public ConsistencyController(ConsistencyService service) { this.service = service; }

    @GetMapping("/{examId}/consistency-flags")
    public ApiResponse<List<ConsistencyFlagResponse>> flags(@PathVariable Long examId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.findFlags(examId, principal));
    }

    @PostMapping("/consistency-flags/{id}/dismiss")
    public ApiResponse<ConsistencyFlagResponse> dismiss(@PathVariable Long id, @Valid @RequestBody ConsistencyReviewRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.dismiss(id, request, principal));
    }

    @PostMapping("/consistency-flags/{id}/resolve")
    public ApiResponse<ConsistencyFlagResponse> resolve(@PathVariable Long id, @Valid @RequestBody ConsistencyReviewRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.resolve(id, request, principal));
    }
}
