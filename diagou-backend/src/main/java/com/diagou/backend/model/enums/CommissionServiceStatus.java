package com.diagou.backend.model.enums;

public enum CommissionServiceStatus {
    PENDING,      // 尚未執行
    IN_PROGRESS,  // 執行中
    COMPLETED,    // 已完成
    SKIPPED       // 跳過（例如客戶臨時取消該服務）
}