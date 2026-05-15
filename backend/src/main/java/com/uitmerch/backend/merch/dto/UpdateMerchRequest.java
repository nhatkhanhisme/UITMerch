package com.uitmerch.backend.merch.dto;

import com.uitmerch.backend.common.model.MerchItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateMerchRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock must be 0 or more")
    private Integer stock;

    @Size(max = 10, message = "A merch item may have at most 10 images")
    private List<@URL(message = "Each image must be a valid URL") String> imageUrls;
    private MerchItemStatus status;
    private String categorySlug;
}
