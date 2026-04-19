package com.diagou.backend.repository;

import com.diagou.backend.model.CommissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionItemRepository extends JpaRepository<CommissionItem, UUID> {
    
    // 找出某張委託單內的所有商品
    List<CommissionItem> findByCommission_Id(UUID commissionId);
}