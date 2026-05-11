package com.uitmerch.backend.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AttachMerchRequest {

    @NotNull(message = "Merch ID is required.")
    private UUID merchId;
}
