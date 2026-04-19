package com.diagou.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.diagou.backend.model.enums.CommissionServiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commission_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", nullable = false)
    private CommissionEntity commission;

    @Column(name = "service_type", nullable = false)
    private String serviceType; // 對應你的 value_added_service_type

    @Column(name = "fee_twd", nullable = false)
    private BigDecimal feeTwd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionServiceStatus status = CommissionServiceStatus.PENDING;

    private OffsetDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String staffNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_image_urls", columnDefinition = "jsonb")
    private java.util.List<String> resultImageUrls;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}