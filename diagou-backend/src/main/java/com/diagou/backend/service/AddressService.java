package com.diagou.backend.service;

import com.diagou.backend.dto.AddressRequest;
import com.diagou.backend.exception.BusinessException;
import com.diagou.backend.exception.ForbiddenException;
import com.diagou.backend.model.AddressEntity;
import com.diagou.backend.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressEntity> listByUser(UUID userId) {
        return addressRepository.findByUserIdAndIsDeletedFalse(userId);
    }

    public AddressEntity create(UUID userId, AddressRequest request) {
        validateRequired(request);

        AddressEntity entity = new AddressEntity();
        entity.setUserId(userId);
        applyFields(entity, request);
        return addressRepository.save(entity);
    }

    public AddressEntity update(UUID addressId, UUID userId, AddressRequest request) {
        validateRequired(request);

        AddressEntity entity = findOwnedAddress(addressId, userId);
        applyFields(entity, request);
        return addressRepository.save(entity);
    }

    public void softDelete(UUID addressId, UUID userId) {
        AddressEntity entity = findOwnedAddress(addressId, userId);
        entity.setIsDeleted(true);
        entity.setIsDefault(false);
        addressRepository.save(entity);
    }

    @Transactional
    public AddressEntity setDefault(UUID addressId, UUID userId) {
        AddressEntity entity = findOwnedAddress(addressId, userId);
        addressRepository.clearDefaultByUserId(userId);
        entity.setIsDefault(true);
        return addressRepository.save(entity);
    }

    private AddressEntity findOwnedAddress(UUID addressId, UUID userId) {
        return addressRepository.findByIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ForbiddenException("地址不存在或無權操作"));
    }

    private void validateRequired(AddressRequest request) {
        if (isBlank(request.getRecipientName())) throw new BusinessException("VALIDATION_ERROR", "收件人姓名為必填");
        if (isBlank(request.getPhone())) throw new BusinessException("VALIDATION_ERROR", "電話為必填");
        if (isBlank(request.getPostalCode())) throw new BusinessException("VALIDATION_ERROR", "郵遞區號為必填");
        if (isBlank(request.getCity())) throw new BusinessException("VALIDATION_ERROR", "縣市為必填");
        if (isBlank(request.getDistrict())) throw new BusinessException("VALIDATION_ERROR", "區域為必填");
        if (isBlank(request.getAddressLine())) throw new BusinessException("VALIDATION_ERROR", "街道地址為必填");
    }

    private void applyFields(AddressEntity entity, AddressRequest request) {
        entity.setLabel(request.getLabel());
        entity.setRecipientName(request.getRecipientName());
        entity.setPhone(request.getPhone());
        entity.setPostalCode(request.getPostalCode());
        entity.setCity(request.getCity());
        entity.setDistrict(request.getDistrict());
        entity.setAddressLine(request.getAddressLine());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
