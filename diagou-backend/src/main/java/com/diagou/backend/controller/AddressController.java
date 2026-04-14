package com.diagou.backend.controller;

import com.diagou.backend.dto.AddressRequest;
import com.diagou.backend.dto.AddressResponse;
import com.diagou.backend.model.AddressEntity;
import com.diagou.backend.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<AddressResponse> responses = addressService.listByUser(userId).stream()
                .map(AddressResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(Authentication authentication,
                                                   @RequestBody AddressRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AddressEntity entity = addressService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponse.from(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @RequestBody AddressRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AddressEntity entity = addressService.update(id, userId, request);
        return ResponseEntity.ok(AddressResponse.from(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                        @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        addressService.softDelete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(Authentication authentication,
                                                       @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        AddressEntity entity = addressService.setDefault(id, userId);
        return ResponseEntity.ok(AddressResponse.from(entity));
    }
}
