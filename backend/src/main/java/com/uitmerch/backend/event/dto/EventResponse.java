package com.uitmerch.backend.event.dto;

import com.uitmerch.backend.common.model.EventStatus;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.merch.dto.MerchResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EventResponse {

    private UUID id;
    private UUID orgId;
    private String title;
    private String description;
    private String coverUrl;
    private EventStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MerchResponse> merch;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
            .id(event.getId())
            .orgId(event.getOrgId())
            .title(event.getTitle())
            .description(event.getDescription())
            .coverUrl(event.getCoverUrl())
            .status(event.getStatus())
            .startsAt(event.getStartsAt())
            .endsAt(event.getEndsAt())
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .merch(null)
            .build();
    }

    public static EventResponse from(Event event, List<MerchResponse> merch) {
        return EventResponse.builder()
            .id(event.getId())
            .orgId(event.getOrgId())
            .title(event.getTitle())
            .description(event.getDescription())
            .coverUrl(event.getCoverUrl())
            .status(event.getStatus())
            .startsAt(event.getStartsAt())
            .endsAt(event.getEndsAt())
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .merch(merch)
            .build();
    }
}
