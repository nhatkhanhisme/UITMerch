package com.uitmerch.backend.event.dto;

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
}
