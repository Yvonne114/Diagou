package com.diagou.backend.dto;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String type;   // "email" or "phone"
    private String target;  // email address or phone number
}
