package com.aes.aiclient.dto;

public record ProcessSheetResponseDto(
	OcrResultDto ocr,
	SegmentationResultDto segmentation,
	ImageQualityDto imageQuality
) {}
