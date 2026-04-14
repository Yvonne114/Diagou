package com.diagou.backend.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String label;
    private String recipientName;
    private String phone;
    private String postalCode;
    private String city;
    private String district;
    private String addressLine;
}
