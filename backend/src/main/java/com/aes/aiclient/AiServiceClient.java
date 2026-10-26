package com.aes.aiclient;

import com.aes.aiclient.dto.EvaluateAnswerRequestDto;
import com.aes.aiclient.dto.EvaluationResultDto;
import com.aes.aiclient.dto.ProcessSheetResponseDto;
import com.aes.common.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@SuppressWarnings("null")
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private static final int MAX_RETRIES = 2;

    private final RestClient restClient;

    public AiServiceClient(
            @Value("${app.ai-service.base-url}") String baseUrl,
            @Value("${app.ai-service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.ai-service.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ProcessSheetResponseDto processAnswerSheet(org.springframework.core.io.Resource fileResource, String expectedQuestionsJson) {
        String requestId = UUID.randomUUID().toString();
        return withRetry(requestId, "process-answer-sheet", () -> {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            if (expectedQuestionsJson != null) {
                body.add("expectedQuestions", expectedQuestionsJson);
            }
            return restClient.post()
                    .uri("/process-answer-sheet")
                    .header("X-Request-Id", requestId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ProcessSheetResponseDto.class);
        });
    }

    public EvaluationResultDto evaluateAnswer(EvaluateAnswerRequestDto request) {
        String requestId = UUID.randomUUID().toString();
        return withRetry(requestId, "evaluate-answer", () -> restClient.post()
                .uri("/evaluate-answer")
                .header("X-Request-Id", requestId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .retrieve()
                .body(EvaluationResultDto.class));
    }

    private <T> T withRetry(String requestId, String operation, java.util.function.Supplier<T> action) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (ResourceAccessException e) {
                // connection refused / timeout - transient, worth retrying
                lastError = e;
                log.warn("requestId={} op={} attempt={}/{} transientFailure={}",
                        requestId, operation, attempt, MAX_RETRIES, e.getMessage());
            } catch (HttpStatusCodeException e) {
                // AI service responded with an error status - not retried, surfaced immediately
                log.error("requestId={} op={} aiServiceStatus={} body={}",
                        requestId, operation, e.getStatusCode(), e.getResponseBodyAsString());
                throw new AiServiceException(
                        "AI service returned " + e.getStatusCode() + " for " + operation, e);
            }
        }
        throw new AiServiceException(
                "AI service unreachable after " + MAX_RETRIES + " attempts for " + operation, lastError);
    }
}
