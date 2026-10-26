package com.aes.rubrics;

import com.aes.common.ApiResponse;
import com.aes.auth.UserPrincipal;
import com.aes.rubrics.dto.RubricCreateRequest;
import com.aes.rubrics.dto.RubricResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rubrics")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RubricResponse> create(@Valid @RequestBody RubricCreateRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(rubricService.create(request, principal), "Rubric created");
    }

    @GetMapping
    public ApiResponse<RubricResponse> getByQuestion(@RequestParam Long questionId,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(rubricService.getByQuestionId(questionId, principal));
    }
}
