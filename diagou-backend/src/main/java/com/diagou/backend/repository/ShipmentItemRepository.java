package com.diagou.backend.repository;

import com.diagou.backend.model.ShipmentEntity;
import com.diagou.backend.model.ShipmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEntity, UUID> {
    List<ShipmentItemEntity> findByShipmentId(UUID shipmentId);
    boolean existsByCommissionItemId(UUID commissionItemId);
}
