package com.aes.admin;

import com.aes.auth.UserPrincipal;
import com.aes.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.dashboard(principal));
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users(@RequestParam(required = false) String search,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "25") int size,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.users(search, page, size, principal));
    }

    @PatchMapping("/users/{userId}/role")
    public ApiResponse<AdminUserResponse> changeRole(@PathVariable Long userId,
                                                       @Valid @RequestBody RoleChangeRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.changeRole(userId, request.role(), principal));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> changeStatus(@PathVariable Long userId,
                                                         @Valid @RequestBody UserStatusChangeRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.changeStatus(userId, request.status(), principal));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AdminAuditResponse>> auditLogs(@RequestParam(required = false) String search,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.auditLogs(search, page, size, principal));
    }

    @GetMapping("/system-health")
    public ApiResponse<SystemHealthResponse> health(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(adminService.health(principal));
    }
}
