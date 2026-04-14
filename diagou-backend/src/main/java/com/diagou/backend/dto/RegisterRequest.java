package com.diagou.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String displayName;
    private String emailCode;  // email 驗證碼
    private String phoneCode;  // phone 驗證碼（選填，有填 phone 時才需要）
}
