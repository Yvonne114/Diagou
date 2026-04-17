package com.diagou.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment_items",uniqueConstraints = @UniqueConstraint(columnNames = {"shipment_id", "commission_item_id"}))
@Data
public class ShipmentItemEntity {
    //PK
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID", nullable = false,updatable = false)
    private UUID id;

    //FK shipments
    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    //FK commission_items, UNIQUE with shipmentId
    @Column(name = "commission_item_id" , nullable = false)
    private UUID commissionItemId;

    @Column(name = "commission_id", nullable = false)
    private UUID commissionId;

    @Column(name = "allocated_shipping_twd")
    private BigDecimal allocatedShippingTwd;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;





}
