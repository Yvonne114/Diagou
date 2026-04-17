package com.diagou.backend.repository;

import com.diagou.backend.model.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {

        List<ShipmentEntity> findByBuyerId(UUID buyerId);
        Optional<ShipmentEntity> findByIdAndBuyerId(UUID id, UUID buyerId);

}



