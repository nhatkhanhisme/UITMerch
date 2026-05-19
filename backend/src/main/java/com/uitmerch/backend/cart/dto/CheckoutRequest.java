package com.uitmerch.backend.cart.dto;

import lombok.Data;

@Data
public class CheckoutRequest {

    private String note;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
}
