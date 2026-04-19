package com.diagou.backend.model.enums;

import lombok.Getter;

@Getter
public enum CancelStage {
    BEFORE_PAID,        // 付款前取消
    BEFORE_PURCHASING,  // 付款後購買前取消
    AFTER_PURCHASING    // 購買後取消
}