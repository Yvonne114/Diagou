package com.diagou.backend.repository;


import com.diagou.backend.model.CommissionService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.diagou.backend.model.enums.CommissionServiceStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionServiceRepository extends JpaRepository<CommissionService, UUID> {

    // 找出所有還沒執行的服務（Staff 用）
    List<CommissionService> findByStatus(CommissionServiceStatus status);
    
    // 找出特定委託單的所有服務執行狀況
    List<CommissionService> findByCommission_Id(UUID commissionId);
}