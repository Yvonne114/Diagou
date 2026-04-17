package com.diagou.backend.service;

import com.diagou.backend.dto.ShipmentItemResponse;
import com.diagou.backend.dto.ShipmentRequest;
import com.diagou.backend.exception.BusinessException;
import com.diagou.backend.exception.ForbiddenException;
import com.diagou.backend.exception.InvalidStateTransitionException;
import com.diagou.backend.model.ShipmentEntity;
import com.diagou.backend.model.ShipmentItemEntity;
import com.diagou.backend.model.enums.ShipmentStatus;
import com.diagou.backend.repository.ShipmentItemRepository;
import com.diagou.backend.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;

    public ShipmentService(ShipmentRepository shipmentRepository, ShipmentItemRepository shipmentItemRepository){
        this.shipmentRepository = shipmentRepository;
        this.shipmentItemRepository = shipmentItemRepository;
    }

    public List<ShipmentEntity> listByBuyer(UUID buyerId){
        return shipmentRepository.findByBuyerId(buyerId);

    }

    public List<ShipmentEntity> listAll(){
        return shipmentRepository.findAll();
    }

    public ShipmentEntity getById(UUID shipmentId, UUID buyerId) {
        if (buyerId == null){
            return shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ForbiddenException(("出貨單不存在")));
        }
        return shipmentRepository.findByIdAndBuyerId(shipmentId, buyerId)
                .orElseThrow(()-> new ForbiddenException("出貨單不存在或無權操作"));
    }

    //查等待出貨的商品
//    public ShipmentEntity getAvailableItems(UUID buyerId){
//
//    }

//    public ShipmentEntity create(UUID buyerId, ShipmentRequest request){
//        validateRequired(request);
//
//        ShipmentEntity entity = new ShipmentEntity();
//        entity.setBuyerId(buyerId);
//        applyFields(entity, request);
//
//        ShipmentEntity saved = shipmentRepository.save(entity);
//
//        // 建立 ShipmentItemEntity
//        for (UUID commissionItemId : request.getCommissionItemIds()){
//            ShipmentItemEntity item = new ShipmentItemEntity();
//            item.setShipmentId(saved.getId());
//            item.setCommissionItemId(commissionItemId);
//            shipmentItemRepository.save(item);
//        }
//        return saved;
//
//    }

    public ShipmentEntity updateStatus(UUID shipmentId, ShipmentStatus newStatus, UUID staffId){

        ShipmentEntity entity = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ForbiddenException("出貨單號不存在或無權操作"));

        validateStatusTransition(entity.getStatus(), newStatus);
        entity.setStatus(newStatus);
        return shipmentRepository.save(entity);

    }

    public ShipmentEntity updateTracking(UUID shipmentId, String trackingNumber, String carrier){

        ShipmentEntity entity = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ForbiddenException("出貨單號不存在或無權操作"));
        entity.setTrackingNumber(trackingNumber);
        entity.setCarrier(carrier);
        return shipmentRepository.save(entity);
    }

    //計算尾款
//    public ShipmentEntity calculateFinalPayment(UUID shipmentId){
//
//    }



    private void validateRequired(ShipmentRequest request) {
        if (request.getShippingAddressId() == null)
            throw new BusinessException("VALIDATION_ERROR", "收件地址為必填");
        if (request.getShippingMethod() == null)
            throw new BusinessException("VALIDATION_ERROR", "運送方式為必填");
        if (request.getCommissionItemIds() == null || request.getCommissionItemIds().isEmpty())
            throw new BusinessException("VALIDATION_ERROR", "至少選擇一個商品");
    }

    private void applyFields(ShipmentEntity entity, ShipmentRequest request) {
        entity.setShippingAddressId(request.getShippingAddressId());
        entity.setShippingMethod(request.getShippingMethod());
    }

    private void validateStatusTransition(ShipmentStatus current, ShipmentStatus next) {
        boolean valid = switch (current) {
            case PREPARING -> next == ShipmentStatus.PACKED || next == ShipmentStatus.EXCEPTION;
            case PACKED -> next == ShipmentStatus.PENDING_PAYMENT || next == ShipmentStatus.EXCEPTION;
            case PENDING_PAYMENT -> next == ShipmentStatus.PAID;
            case PAID -> next == ShipmentStatus.SHIPPED || next == ShipmentStatus.EXCEPTION;
            case SHIPPED -> next == ShipmentStatus.IN_TRANSIT || next == ShipmentStatus.EXCEPTION;
            case IN_TRANSIT -> next == ShipmentStatus.CUSTOMS || next == ShipmentStatus.DELIVERED || next == ShipmentStatus.EXCEPTION;
            case CUSTOMS -> next == ShipmentStatus.DELIVERED || next == ShipmentStatus.EXCEPTION;
            default -> false;
        };

        if (!valid)
            throw new InvalidStateTransitionException(current.name(), next.name());
    }

}
