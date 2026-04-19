package com.diagou.backend.model.enums;

public enum CommissionStatus {
    PENDING,            // Buyer 已提交，等 Staff 審核
    CONFIRMED,          // Staff 確認，等 Buyer 預付款
    PAID,               // Buyer 已預付，等 Staff 購買
    PURCHASING,         // Staff 正在日本購買
    IN_JP_WAREHOUSE,    // 商品已抵日本倉庫
    WAITING_SHIPMENT,   // 等待合併出貨
    SHIPPED,            // 已進出貨單且出貨
    DELIVERED,          // Buyer 確認收到
    REJECTED,           // Staff 拒絕
    CANCELLED           // Buyer 取消
}