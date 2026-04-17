package com.diagou.backend.dto;

import com.diagou.backend.model.ShipmentItemEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ShipmentItemResponse {
    private UUID id;
    private UUID shipmentId;
    private UUID commissionItemId;
    private UUID commissionId;
    private BigDecimal allocatedShippingTwd;
    private OffsetDateTime createdAt;

    public static ShipmentItemResponse from(ShipmentItemEntity entity){
        return ShipmentItemResponse.builder()
                .id(entity.getId())
                .shipmentId(entity.getShipmentId())
                .commissionItemId(entity.getCommissionItemId())
                .commissionId(entity.getCommissionId())
                .allocatedShippingTwd(entity.getAllocatedShippingTwd())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
