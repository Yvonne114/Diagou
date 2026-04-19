package com.diagou.backend.dto;

import com.diagou.backend.model.enums.CommissionStatus;
import com.diagou.backend.model.enums.CommissionServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionResponse {

    private UUID id;
    private CommissionStatus status;
    private String cancelStage;
    
    // 費用快照
    private BigDecimal itemsCostJpy;
    private BigDecimal itemsCostTwd;
    private BigDecimal jpyToTwdRate;
    private BigDecimal serviceFeeTwd;
    private BigDecimal inspectionFeeTwd;
    private BigDecimal prepayTotalTwd;

    // 備註
    private String buyerNote;
    private String staffNote;

    // 關鍵時間戳
    private OffsetDateTime submittedAt;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime updatedAt;

    // 關聯資料
    private List<ItemResponse> items;
    private List<ServiceResponse> services;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private String productUrl;
        private String productNameJa;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal unitPriceActualJpy;
        private Integer weightActualG;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceResponse {
        private String serviceType;
        private BigDecimal feeTwd;
        private CommissionServiceStatus status;
        private OffsetDateTime completedAt;
    }
}