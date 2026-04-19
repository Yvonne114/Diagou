package com.diagou.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.diagou.backend.model.enums.CommissionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commission_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", nullable = false)
    private CommissionEntity commission;

    @Column(name = "product_url", nullable = false)
    private String productUrl;

    @Column(name = "product_name_ja")
    private String productNameJa;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "jsonb")
    private java.util.Map<String, Object> specifications;

    @Column(nullable = false)
    private Short quantity;

    private BigDecimal unitPriceActualJpy;
    
    @Column(name = "weight_actual_g")
    private Integer weightActualG;

    @Column(columnDefinition = "TEXT")
    private String itemNote;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}