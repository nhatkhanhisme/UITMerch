package com.uitmerch.backend.event.dto;

import com.uitmerch.backend.common.model.EventStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    @NotBlank(message = "Title is required.")
    private String title;

    private String description;

    private String coverUrl;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    /** Optional. Defaults to DRAFT when omitted. ENDED and CANCELLED are not allowed on create. */
    private EventStatus status;
}
