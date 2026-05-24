package com.uitmerch.backend.ai.dto;

import com.uitmerch.backend.merch.dto.MerchResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchWithSimilarity {
    private MerchResponse merch;
    private int similarity;
}
