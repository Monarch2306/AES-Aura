package com.aes.analytics;

import com.aes.analytics.dto.ClassAnalyticsResponse;
import com.aes.analytics.dto.StudentAnalyticsResponse;
import com.aes.analytics.dto.ExamAnalyticsResponse;
import com.aes.auth.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aes.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;

    public AnalyticsController(AnalyticsService analyticsService, ReportExportService reportExportService) {
        this.analyticsService = analyticsService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/student/{id}")
    public ApiResponse<StudentAnalyticsResponse> student(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(analyticsService.studentAnalytics(id, principal));
    }

    @GetMapping("/class/{examId}")
    public ApiResponse<ClassAnalyticsResponse> classAnalytics(@PathVariable Long examId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(analyticsService.classAnalytics(examId, principal));
    }

    @GetMapping("/exam/{examId}")
    public ApiResponse<ExamAnalyticsResponse> examAnalytics(@PathVariable Long examId,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(analyticsService.examAnalytics(examId, principal));
    }

    @GetMapping("/exam/{examId}/export")
    public org.springframework.http.ResponseEntity<byte[]> exportExam(@PathVariable Long examId,
                                                                      @RequestParam(defaultValue = "csv") String format,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        byte[] data;
        String filename;
        String mediaType;
        if ("csv".equalsIgnoreCase(format)) {
            data = reportExportService.generateExamCsv(examId, principal);
            filename = "exam_" + examId + "_report.csv";
            mediaType = "text/csv";
        } else {
            data = reportExportService.generateExamReportTxt(examId, principal);
            filename = "exam_" + examId + "_report.txt";
            mediaType = "text/plain";
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, mediaType)
                .body(data);
    }

    @GetMapping("/student/{studentId}/export")
    public org.springframework.http.ResponseEntity<byte[]> exportStudent(@PathVariable Long studentId,
                                                                         @AuthenticationPrincipal UserPrincipal principal) {
        byte[] data = reportExportService.generateStudentReportTxt(studentId, principal);
        String filename = "student_" + studentId + "_report.txt";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/plain")
                .body(data);
    }
}
