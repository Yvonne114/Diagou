package com.diagou.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.diagou.backend.model.enums.ValueAddedServiceType;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRequest {

    private String buyerNote;
    private Boolean requiresInspection;
    private List<ItemRequest> items;
    private List<ValueAddedServiceType> serviceTypes;  

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private String productUrl;
        private String productNameJa;
        private String productImageUrl;
        private Map<String, Object> specifications; // 對應 JSONB
        private Short quantity;
        private String itemNote;
    }
}