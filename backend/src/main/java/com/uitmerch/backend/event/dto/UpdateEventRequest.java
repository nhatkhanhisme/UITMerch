package com.uitmerch.backend.event.dto;

import com.uitmerch.backend.common.model.EventStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventRequest {

    private String title;

    private String description;

    private String coverUrl;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private EventStatus status;
}
