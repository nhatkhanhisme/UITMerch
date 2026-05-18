package com.uitmerch.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GuestOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    private List<GuestOrderItemRequest> items;

    @NotBlank(message = "Guest name is required")
    private String guestName;

    @NotBlank(message = "Guest phone is required")
    private String guestPhone;

    // Optional — campus pickup model; no shipping address needed.
    private String guestAddress;

    private String guestEmail;

    private String note;
}
