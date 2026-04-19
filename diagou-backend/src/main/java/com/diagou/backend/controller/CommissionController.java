package com.diagou.backend.controller;

import com.diagou.backend.dto.CommissionRequest;
import com.diagou.backend.dto.CommissionResponse;
import com.diagou.backend.model.CommissionEntity;
import com.diagou.backend.repository.CommissionRepository;
import com.diagou.backend.service.CommissionCommandService;
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
    private final CommissionCommandService commissionCommandService;

    // // 1. Buyer 提交新委託單
    // @PostMapping
    // public ResponseEntity<UUID> createCommission(
    //         @RequestBody CommissionRequest request,
    //         @AuthenticationPrincipal UserDetails user) {
    //     UUID buyerId = UUID.fromString(user.getUsername());
    //     UUID id = commissionCommandService.createCommission(buyerId, request);
    //     return ResponseEntity.ok(id);
    // }

    // 1. Buyer 提交新委託單
    //測試用
    @PostMapping
    public ResponseEntity<?> createCommission(
            @RequestBody CommissionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        // 解決 500 NPE 的關鍵：如果沒有登入，給一個測試用的固定 UUID
        UUID buyerId;
        if (user != null) {
            buyerId = UUID.fromString(user.getUsername());
        } else {
            // 測試用：發送請求時若沒帶 Token，會預設使用這組 UUID
            buyerId = UUID.fromString("0e51f4ec-5852-4919-a65a-84e361c591e8");
        }

        // 請確認你的 Service 方法名是 createCommission 還是 create
        // 這裡我們統一使用你原本定義的邏輯
        UUID id = commissionCommandService.createCommission(buyerId, request);
        return ResponseEntity.ok(id);
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
                        .quantity(i.getQuantity().intValue())
                        .unitPriceActualJpy(i.getUnitPriceActualJpy())
                        .build()
                ).collect(Collectors.toList()))
                .build();
    }
}