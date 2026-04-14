package com.diagou.backend.model;

import com.diagou.backend.model.enums.PaymentMethod;
import com.diagou.backend.model.enums.PaymentStatus;
import com.diagou.backend.model.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"commission_id", "payment_type"}))
@Data
public class PaymentEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 20)
    private String paymentNumber;

    @Column(name = "commission_id")
    private UUID commissionId;

    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "amount_twd", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTwd;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_transaction_id", length = 200)
    private String gatewayTransactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    @Column(name = "ecpay_payment_type", length = 30)
    private String ecpayPaymentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atm_info", columnDefinition = "jsonb")
    private Map<String, Object> atmInfo;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "refund_amount_twd", precision = 12, scale = 2)
    private BigDecimal refundAmountTwd;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_breakdown", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> paymentBreakdown = Map.of();

    @Column(name = "confirmed_by_staff_id")
    private UUID confirmedByStaffId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
