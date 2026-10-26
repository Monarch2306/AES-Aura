package com.aes.common;

/** Thrown when the Python AI/CV service is unreachable, times out, or
 * returns an error. Caught centrally so the API never crashes and always
 * returns a meaningful, structured error instead (Phase 15 requirement). */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) { super(message); }
    public AiServiceException(String message, Throwable cause) { super(message, cause); }
}
