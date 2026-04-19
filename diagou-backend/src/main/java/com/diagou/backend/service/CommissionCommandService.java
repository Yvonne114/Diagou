package com.diagou.backend.service;

import com.diagou.backend.dto.CommissionRequest;
import com.diagou.backend.model.CommissionEntity;
import com.diagou.backend.model.CommissionItem;
import com.diagou.backend.model.CommissionService;
import com.diagou.backend.model.enums.CommissionStatus;
import com.diagou.backend.repository.CommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diagou.backend.model.enums.CommissionServiceStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionCommandService {

    private final CommissionRepository commissionRepository;

    /**
     * Buyer 建立委託單
     * 初始狀態為 PENDING
     */
    @Transactional
    public UUID createCommission(UUID buyerId, CommissionRequest request) {
        // 1. 建立主表 Entity
        CommissionEntity commission = CommissionEntity.builder()
                .buyerId(buyerId)
                .status(CommissionStatus.PENDING)
                .buyerNote(request.getBuyerNote())
                .requiresInspection(request.getRequiresInspection())
                .build();

        // 2. 轉換並建立商品明細 (Items)
        List<CommissionItem> items = request.getItems().stream().map(itemReq -> {
            CommissionItem item = new CommissionItem();
            item.setCommission(commission);
            item.setProductUrl(itemReq.getProductUrl());
            item.setProductNameJa(itemReq.getProductNameJa());
            item.setProductImageUrl(itemReq.getProductImageUrl());
            item.setSpecifications(itemReq.getSpecifications());
            item.setQuantity(itemReq.getQuantity());
            item.setItemNote(itemReq.getItemNote());
            return item;
        }).collect(Collectors.toList());

        commission.setItems(items);

        // 3. 處理預約的服務 (Services)
        // 這裡可以根據 request.getServiceTypes() 去抓取資料庫預設費率
        if (request.getServiceTypes() != null) {
            List<CommissionService> services = request.getServiceTypes().stream().map(type -> {
                CommissionService service = new CommissionService();
                service.setCommission(commission);
                service.setServiceType(type);
                service.setStatus(CommissionServiceStatus.PENDING);
                service.setFeeTwd(BigDecimal.ZERO); // 初始為0，待 Staff 確認後填入
                return service;
            }).collect(Collectors.toList());
            commission.setServices(services);
        }

        // 4. 存檔並回傳 ID
        CommissionEntity saved = commissionRepository.save(commission);
        return saved.getId();
    }

    /**
     * Staff 確認委託單
     * 此時會填入日幣金額與匯率，並計算台幣預付金額
     */
    @Transactional
    public void confirmCommission(UUID commissionId, UUID staffId, BigDecimal rate, BigDecimal itemsJpy) {
        CommissionCommissionEntity commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new RuntimeException("委託單不存在"));

        // 檢查狀態
        if (commission.getStatus() != CommissionStatus.PENDING) {
            throw new IllegalStateException("只有 PENDING 狀態的單子可以確認");
        }

        // 1. 設定工作人員與匯率
        commission.setAssignedStaffId(staffId);
        commission.setJpyToTwdRate(rate);
        commission.setItemsCostJpy(itemsJpy);

        // 2. 計算商品台幣價格 (Jpy * Rate)
        BigDecimal itemsTwd = itemsJpy.multiply(rate).setScale(0, BigDecimal.ROUND_HALF_UP);
        commission.setItemsCostTwd(itemsTwd);

        // 3. 計算預付總額 (商品台幣 + 服務費 + 檢驗費)
        // 假設目前服務費由 Staff 手動填入或由其他邏輯計算
        BigDecimal serviceFee = commission.getServiceFeeTwd() != null ? commission.getServiceFeeTwd() : BigDecimal.ZERO;
        BigDecimal inspectionFee = commission.getInspectionFeeTwd() != null ? commission.getInspectionFeeTwd() : BigDecimal.ZERO;
        
        BigDecimal total = itemsTwd.add(serviceFee).add(inspectionFee);
        commission.setPrepayTotalTwd(total);

        // 4. 更新狀態與確認時間
        commission.setStatus(CommissionStatus.CONFIRMED);
        commission.setConfirmedAt(java.time.OffsetDateTime.now());

        commissionRepository.save(commission);
    }

    /**
     * 更新付款狀態
     */
    @Transactional
    public void markAsPaid(UUID commissionId) {
        CommissionEntity commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new RuntimeException("委託單不存在"));

        commission.setStatus(CommissionStatus.PAID);
        commission.setPaidAt(java.time.OffsetDateTime.now());
        
        commissionRepository.save(commission);
    }
}