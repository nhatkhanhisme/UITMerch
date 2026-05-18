package com.uitmerch.backend.order.dto;

import com.uitmerch.backend.order.entity.PickupSchedule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PickupScheduleResponse {

    private UUID id;
    private UUID orgId;
    private LocalDate pickupDate;
    private String pickupTimeSlot;
    private String location;
    private String notes;
    private int orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PickupScheduleResponse from(PickupSchedule schedule, int orderCount) {
        return PickupScheduleResponse.builder()
            .id(schedule.getId())
            .orgId(schedule.getOrgId())
            .pickupDate(schedule.getPickupDate())
            .pickupTimeSlot(schedule.getPickupTimeSlot())
            .location(schedule.getLocation())
            .notes(schedule.getNotes())
            .orderCount(orderCount)
            .createdAt(schedule.getCreatedAt())
            .updatedAt(schedule.getUpdatedAt())
            .build();
    }
}
