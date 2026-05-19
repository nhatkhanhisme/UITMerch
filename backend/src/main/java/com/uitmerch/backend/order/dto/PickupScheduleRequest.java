package com.uitmerch.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Create a campus pickup schedule for a batch of confirmed orders")
public class PickupScheduleRequest {

    @NotNull(message = "Pickup date is required")
    @Schema(example = "2026-06-15")
    private LocalDate pickupDate;

    @NotBlank(message = "Pickup time slot is required")
    @Size(max = 100)
    @Schema(example = "08:00 – 11:30")
    private String pickupTimeSlot;

    @NotBlank(message = "Location is required")
    @Size(max = 500)
    @Schema(example = "Phòng B1-01, Toà nhà B, ĐH Công nghệ Thông tin")
    private String location;

    @Size(max = 1000)
    @Schema(description = "Optional extra instructions for students")
    private String notes;

    @NotEmpty(message = "At least one CONFIRMED order must be assigned to the schedule")
    @Schema(description = "IDs of CONFIRMED orders to include in this pickup schedule")
    private List<UUID> orderIds;
}
