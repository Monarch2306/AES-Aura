package com.aes.similarity;

import jakarta.validation.constraints.Size;

public record SimilarityReviewRequest(@Size(max = 2000) String notes) {}
