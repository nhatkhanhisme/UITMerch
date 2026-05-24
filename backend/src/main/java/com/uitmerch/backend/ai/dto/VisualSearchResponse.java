package com.uitmerch.backend.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VisualSearchResponse {
    private String aiDescription;
    private List<MerchWithSimilarity> results;
}
