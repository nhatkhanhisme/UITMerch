package com.uitmerch.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Cancel order request with mandatory reason")
public class CancelOrderRequest {

    @NotBlank(message = "Cancel reason is required")
    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    @Schema(example = "Tôi đặt nhầm sản phẩm / số lượng")
    private String cancelReason;

    @Size(min = 10, max = 1000, message = "Note must be at least 10 characters when provided")
    @Schema(description = "Required when cancelReason is 'Lý do khác' — min 10 characters")
    private String cancelReasonNote;
}
