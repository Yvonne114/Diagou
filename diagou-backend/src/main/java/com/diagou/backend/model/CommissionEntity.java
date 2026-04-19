package com.diagou.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.diagou.backend.model.enums.CommissionStatus;
import com.diagou.backend.model.enums.CancelStage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "commission_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CommissionStatus status;

    // 如果你還沒定義 CancelStage Enum，請先建立一個
    // 如果暫時不想建立 Enum，請看下方的「快速繞過法」

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_stage", columnDefinition = "cancel_stage")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // 這是讓 PostgreSQL 乖乖轉型的關鍵
    private CancelStage cancelStage;

    // @Column(name = "requires_inspection", nullable = false)
    // private Boolean requiresInspection = false;

    @Column(nullable = false)
    private Boolean requiresInspection = false; // 給它一個預設值

    @Column(name = "buyer_note", columnDefinition = "TEXT")
    private String buyerNote;

    @Column(name = "staff_note", columnDefinition = "TEXT")
    private String staffNote;

    // 拒絕相關
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    // 取消相關
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    // 費用欄位 (日圓建議用 BigDecimal 確保精準度)
    private BigDecimal itemsCostJpy;
    private BigDecimal itemsCostTwd;
    private BigDecimal jpyToTwdRate;
    private BigDecimal serviceFeeTwd;
    private BigDecimal inspectionFeeTwd;
    private BigDecimal prepayTotalTwd;

    // 階段時間戳
    @CreationTimestamp
    private OffsetDateTime submittedAt;

    private OffsetDateTime confirmedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime purchasingStartedAt;
    private OffsetDateTime arrivedWarehouseAt;
    private OffsetDateTime deliveredAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    // 關聯設定
    @OneToMany(mappedBy = "commission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommissionItem> items;

    @OneToMany(mappedBy = "commission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommissionService> services;
}