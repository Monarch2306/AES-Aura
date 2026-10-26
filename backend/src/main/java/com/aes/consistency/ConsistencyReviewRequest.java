package com.aes.consistency;

import jakarta.validation.constraints.Size;

public record ConsistencyReviewRequest(@Size(max = 2000) String notes) {}
