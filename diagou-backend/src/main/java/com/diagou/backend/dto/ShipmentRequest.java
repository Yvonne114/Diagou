package com.diagou.backend.dto;

import com.diagou.backend.model.enums.ShippingMethod;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ShipmentRequest {
    private UUID shippingAddressId;
    private ShippingMethod shippingMethod;
    private List<UUID> commissionItemIds;

}
