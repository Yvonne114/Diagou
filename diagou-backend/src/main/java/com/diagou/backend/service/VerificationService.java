package com.diagou.backend.service;

import com.diagou.backend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final int CODE_LENGTH = 6;
    private static final long EXPIRY_SECONDS = 300; // 5 minutes

    private record CodeEntry(String code, Instant expiresAt) {}

    // key = "email:xxx@gmail.com" or "phone:0912345678"
    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void sendCode(String type, String target) {
        String code = generateCode();
        String key = type + ":" + target;
        store.put(key, new CodeEntry(code, Instant.now().plusSeconds(EXPIRY_SECONDS)));

        // 不真的寄出，印在 console
        log.info("========================================");
        log.info("驗證碼 [{}] → {} : {}", type, target, code);
        log.info("========================================");
    }

    public void verify(String type, String target, String code) {
        String key = type + ":" + target;
        CodeEntry entry = store.get(key);

        if (entry == null) {
            throw new BusinessException("INVALID_CODE", "請先發送驗證碼");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            throw new BusinessException("CODE_EXPIRED", "驗證碼已過期，請重新發送");
        }
        if (!entry.code().equals(code)) {
            throw new BusinessException("INVALID_CODE", "驗證碼錯誤");
        }

        store.remove(key);
    }

    public void check(String type, String target, String code) {
        String key = type + ":" + target;
        CodeEntry entry = store.get(key);

        if (entry == null) {
            throw new BusinessException("INVALID_CODE", "請先發送驗證碼");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            throw new BusinessException("CODE_EXPIRED", "驗證碼已過期，請重新發送");
        }
        if (!entry.code().equals(code)) {
            throw new BusinessException("INVALID_CODE", "驗證碼錯誤");
        }
        // 不移除，讓後續 register/update 時再消耗
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
