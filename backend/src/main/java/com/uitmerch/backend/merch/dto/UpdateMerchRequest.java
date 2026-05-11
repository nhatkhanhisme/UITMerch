package com.uitmerch.backend.merch.dto;

import com.uitmerch.backend.common.model.MerchItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateMerchRequest {

    private String name;
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock must be 0 or more")
    private Integer stock;

    private String imageUrl;
    private MerchItemStatus status;
}
