package com.aes.admin;

import java.time.Instant;

public record SystemHealthResponse(String backend, String database, String aiService,
                                   String ocr, String evaluationApi, String storage,
                                   String applicationVersion, String environment,
                                   Instant checkedAt) {}
