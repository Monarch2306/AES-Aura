package com.aes.admin;

import java.time.Instant;

public record AdminAuditResponse(Long id, Long actorUserId, String action, String entityType,
                                 Long entityId, String details, Instant timestamp) {}
