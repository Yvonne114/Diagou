package com.diagou.backend.model;

import com.diagou.backend.model.enums.ShipmentStatus;
import com.diagou.backend.model.enums.ShippingMethod;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Data
public class ShipmentEntity {
    //PK
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID" , updatable = false, nullable = false)
    private UUID id;

    //FK buyer
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    //FK staff
    @Column(name = "created_by_staff_id")
    private UUID createdByStaffId;

    //FK address
    @Column(name = "shipping_address_id", nullable = false)
    private  UUID shippingAddressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.PREPARING;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false)
    private ShippingMethod shippingMethod = ShippingMethod.AIR_STANDARD;

    @Column(name = "total_weight_g")
    private Integer totalWeightG;

    @Column(name = "domestic_shipping_twd")
    private BigDecimal domesticShippingTwd;

    @Column(name = "intl_shipping_twd")
    private BigDecimal intlShippingTwd;

    @Column(name = "customs_twd")
    private BigDecimal customsTwd;

    @Column(name = "final_total_twd")
    private BigDecimal finalTotalTwd;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column
    private String carrier;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "estimated_delivery_at")
    private LocalDate estimatedDeliveryAt;

    @CreationTimestamp
    @Column(name = "created_at",updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;







}
