package com.diagou.backend.controller;


import com.diagou.backend.dto.ShipmentRequest;
import com.diagou.backend.dto.ShipmentResponse;
import com.diagou.backend.model.ShipmentEntity;
import com.diagou.backend.model.enums.ShipmentStatus;
import com.diagou.backend.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController (ShipmentService shipmentService){
        this.shipmentService = shipmentService;
    }

//    @GetMapping("/available-item")
//    public ResponseEntity<?> listgetAvailableItems(Authentication authentication){
//
//    }

//    @PostMapping
//    public ResponseEntity<ShipmentResponse> create(Authentication authentication, @RequestBody ShipmentRequest request){
//
//        UUID buyerId = (UUID) authentication.getPrincipal();
//        ShipmentEntity entity = shipmentService.create(buyerId, request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(ShipmentResponse.from(entity));
//    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>>  list(Authentication authentication){
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        List<ShipmentEntity> entities;
        if (role.equals("ROLE_BUYER")) {
            entities = shipmentService.listByBuyer(userId);
        }
        else {
            entities = shipmentService.listAll();
        }

        List<ShipmentResponse> responses = entities.stream()
                .map(ShipmentResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getById(Authentication authentication, @PathVariable UUID id){
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        ShipmentEntity entity;
        if (role.equals("ROLE_BUYER")){
            entity = shipmentService.getById(id, userId);
        }
        else {
            entity = shipmentService.getById(id, null);
        }
        return ResponseEntity.ok(ShipmentResponse.from(entity));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateStatus(Authentication authentication, @PathVariable UUID id, @RequestBody Map<String ,String > body){
        UUID staffId = (UUID) authentication.getPrincipal();
        ShipmentStatus newStatus = ShipmentStatus.valueOf(body.get("status"));
        ShipmentEntity entity = shipmentService.updateStatus(id, newStatus, staffId);

        return ResponseEntity.ok(ShipmentResponse.from(entity));
    }

    @PatchMapping("/{id}/tracking")
    public ResponseEntity<ShipmentResponse> updateTracking(@PathVariable UUID id,  @RequestBody Map<String ,String > body){

        String trackingNumber = body.get("trackingNumber");
        String carrier = body.get("carrier");
        ShipmentEntity entity = shipmentService.updateTracking(id, trackingNumber, carrier);

        return ResponseEntity.ok(ShipmentResponse.from(entity));
    }

}
