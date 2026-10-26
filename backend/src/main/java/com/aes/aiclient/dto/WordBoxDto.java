package com.aes.aiclient.dto;

public record WordBoxDto(String text, Double confidence, Integer left, Integer top, Integer width, Integer height) {}
