package com.diagou.backend.controller;

import com.diagou.backend.dto.SendCodeRequest;
import com.diagou.backend.dto.VerifyCodeRequest;
import com.diagou.backend.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendCode(@RequestBody SendCodeRequest request) {
        verificationService.sendCode(request.getType(), request.getTarget());
        return ResponseEntity.ok(Map.of("message", "驗證碼已發送（請查看後端 console）"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody VerifyCodeRequest request) {
        verificationService.check(request.getType(), request.getTarget(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "驗證成功"));
    }
}
