package com.uitmerch.backend.cart.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private java.util.UUID requestId;

    private String note;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
}
