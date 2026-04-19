package com.diagou.backend.controller;

import com.diagou.backend.dto.CommissionRequest;
import com.diagou.backend.dto.CommissionResponse;
import com.diagou.backend.model.CommissionEntity;
import com.diagou.backend.repository.CommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionRepository commissionRepository;

    // 1. Buyer 提交新委託單
    @PostMapping
    public ResponseEntity<UUID> createCommission(@RequestBody CommissionRequest request) {
        // 這裡通常會呼叫 Service 處理：
        // 1. 轉換 DTO 為 Entity
        // 2. 設置初始狀態為 PENDING
        // 3. 處理關聯的 Items & Services
        // 4. 存檔並回傳 ID
        return ResponseEntity.ok(UUID.randomUUID()); // 暫代
    }

    // 2. 獲取特定委託單詳情 (Buyer/Staff 共用)
    @GetMapping("/{id}")
    public ResponseEntity<CommissionResponse> getCommission(@PathVariable UUID id) {
        return commissionRepository.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. 獲取當前用戶的所有委託
    @GetMapping("/my")
    public ResponseEntity<List<CommissionResponse>> getMyCommissions(@AuthenticationPrincipal UserDetails user) {
        UUID buyerId = UUID.fromString(user.getUsername());
        List<CommissionResponse> list = commissionRepository.findByBuyerIdOrderBySubmittedAtDesc(buyerId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // 私有轉換邏輯 (之後可改用 MapStruct 等工具優化)
    private CommissionResponse convertToResponse(CommissionEntity entity) {
        return CommissionResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .itemsCostJpy(entity.getItemsCostJpy())
                .itemsCostTwd(entity.getItemsCostTwd())
                .jpyToTwdRate(entity.getJpyToTwdRate())
                .prepayTotalTwd(entity.getPrepayTotalTwd())
                .submittedAt(entity.getSubmittedAt())
                .items(entity.getItems().stream().map(i -> 
                    CommissionResponse.ItemResponse.builder()
                        .productUrl(i.getProductUrl())
                        .productNameJa(i.getProductNameJa())
                        .quantity(i.getQuantity())
                        .unitPriceActualJpy(i.getUnitPriceActualJpy())
                        .build()
                ).collect(Collectors.toList()))
                .build();
    }
}