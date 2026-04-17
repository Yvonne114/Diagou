package com.diagou.backend.dto;

import com.diagou.backend.model.ShipmentEntity;
import com.diagou.backend.model.enums.ShipmentStatus;
import com.diagou.backend.model.enums.ShippingMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ShipmentResponse {

    private UUID id;
    private UUID buyerId;
    private UUID createdByStaffId;
    private UUID shippingAddressId;
    private ShipmentStatus status;
    private ShippingMethod shippingMethod;
    private Integer totalWeightG;
    private BigDecimal domesticShippingTwd;
    private BigDecimal intlShippingTwd;
    private BigDecimal customsTwd;
    private BigDecimal finalTotalTwd;
    private String trackingNumber;
    private String carrier;
    private OffsetDateTime shippedAt;
    private OffsetDateTime deliveredAt;
    private LocalDate estimatedDeliveryAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ShipmentResponse from(ShipmentEntity entity){
        return ShipmentResponse.builder()
                .id(entity.getId())
                .buyerId(entity.getBuyerId())
                .createdByStaffId(entity.getCreatedByStaffId())
                .shippingAddressId(entity.getShippingAddressId())
                .status(entity.getStatus())
                .shippingMethod(entity.getShippingMethod())
                .totalWeightG(entity.getTotalWeightG())
                .domesticShippingTwd((entity.getDomesticShippingTwd()))
                .intlShippingTwd(entity.getIntlShippingTwd())
                .customsTwd(entity.getCustomsTwd())
                .finalTotalTwd(entity.getFinalTotalTwd())
                .trackingNumber(entity.getTrackingNumber())
                .carrier(entity.getCarrier())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .estimatedDeliveryAt(entity.getEstimatedDeliveryAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
