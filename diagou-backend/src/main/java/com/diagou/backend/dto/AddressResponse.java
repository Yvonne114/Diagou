package com.diagou.backend.dto;

import com.diagou.backend.model.AddressEntity;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {
    private UUID id;
    private String label;
    private String recipientName;
    private String phone;
    private String postalCode;
    private String city;
    private String district;
    private String addressLine;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AddressResponse from(AddressEntity entity) {
        return AddressResponse.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .recipientName(entity.getRecipientName())
                .phone(entity.getPhone())
                .postalCode(entity.getPostalCode())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .addressLine(entity.getAddressLine())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
