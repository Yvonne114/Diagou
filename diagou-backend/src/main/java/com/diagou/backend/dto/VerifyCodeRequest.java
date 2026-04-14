package com.diagou.backend.dto;

import lombok.Data;

@Data
public class VerifyCodeRequest {
    private String type;
    private String target;
    private String code;
}
