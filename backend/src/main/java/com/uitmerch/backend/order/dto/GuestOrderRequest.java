package com.uitmerch.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class GuestOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 100)
    @Valid
    private List<@NotNull GuestOrderItemRequest> items;

    @NotBlank(message = "Guest name is required")
    @Size(max = 255)
    private String guestName;

    @NotBlank(message = "Guest phone is required")
    @Size(max = 20)
    @Pattern(regexp = "[+0-9 ()-]{7,20}", message = "Invalid phone number")
    private String guestPhone;

    // Optional — campus pickup model; no shipping address needed.
    @Size(max = 1000)
    private String guestAddress;

    @Email
    @Size(max = 255)
    private String guestEmail;

    @Size(max = 1000)
    private String note;
}
