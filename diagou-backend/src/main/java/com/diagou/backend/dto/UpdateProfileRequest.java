package com.diagou.backend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String displayName;
    private String email;
    private String phone;
    private String emailCode;  // 修改 email 時需要
    private String phoneCode;  // 修改 phone 時需要
}
