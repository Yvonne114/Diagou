package com.diagou.backend.repository;

import com.diagou.backend.model.CommissionEntity;
import com.diagou.backend.model.enums.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionRepository extends JpaRepository<CommissionEntity, UUID> {

    // 1. Buyer 查詢自己的委託清單（按提交時間排序）
    List<CommissionEntity> findByBuyerIdOrderBySubmittedAtDesc(UUID buyerId);

    // 2. Staff 查詢特定狀態的單子（例如：查詢所有 PENDING 待審核的單）
    List<CommissionEntity> findByStatus(CommissionStatus status);

    // 3. Staff 查詢自己負責且正在採購中的單子
    List<CommissionEntity> findByAssignedStaffIdAndStatus(UUID staffId, CommissionStatus status);

    // 4. 複合查詢：查詢特定 Buyer 且處於「待付款」狀態的單
    List<CommissionEntity> findByBuyerIdAndStatus(UUID buyerId, CommissionStatus status);
}