package com.aes.common;

import com.aes.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    private String newRequestId() {
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} notFound={}", rid, req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage(), rid));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(BadRequestException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} badRequest={}", rid, req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), rid));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> aiServiceDown(AiServiceException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.error("requestId={} path={} aiServiceError={}", rid, req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("AI evaluation service is currently unavailable: " + ex.getMessage(), rid));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String rid = newRequestId();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("requestId={} path={} validationErrors={}", rid, req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + fieldErrors, rid));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> uploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} uploadTooLarge", rid, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Uploaded file exceeds the maximum allowed size", rid));
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        String rid = newRequestId();
        auditService.log(null, "FORBIDDEN_ACCESS", "HTTP", null, req.getMethod() + " " + req.getRequestURI());
        log.warn("requestId={} path={} accessDenied", rid, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied", rid));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} badCredentials", rid, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid email or password", rid));
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} dataIntegrityViolation", rid, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The request conflicts with existing data", rid));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        String rid = newRequestId();
        log.warn("requestId={} path={} methodNotAllowed method={}", rid, req.getRequestURI(), req.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("HTTP method " + req.getMethod() + " is not supported for this endpoint", rid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> generic(Exception ex, HttpServletRequest req) {
        String rid = newRequestId();
        // Never leak stack traces / internals to the client (Phase 16).
        log.error("requestId={} path={} unhandledException", rid, req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Reference: " + rid, rid));
    }
}
