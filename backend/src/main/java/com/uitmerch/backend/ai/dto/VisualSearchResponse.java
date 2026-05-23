package com.uitmerch.backend.ai.dto;

import com.uitmerch.backend.merch.dto.MerchResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VisualSearchResponse {
    private String aiDescription;
    private List<MerchResponse> results;
}
