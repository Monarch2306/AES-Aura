package com.aes.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
    private final String requestId;
    private final Instant timestamp = Instant.now();

    private ApiResponse(boolean success, T data, String message, String requestId) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.requestId = requestId;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String requestId) {
        return new ApiResponse<>(false, null, message, requestId);
    }
}
