package com.csms.csms.controller;

import com.csms.csms.entity.BradaPurchase;
import com.csms.csms.repository.BradaPurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brada")
@CrossOrigin(origins = "*")
public class BradaController {

    @Autowired
    private BradaPurchaseRepository bradaPurchaseRepository;

    // ===== PURCHASES =====

    @PostMapping("/purchases")
    public ResponseEntity<BradaPurchase> createPurchase(@RequestBody BradaPurchaseRequest request) {
        BradaPurchase purchase = new BradaPurchase(
    request.getFlockId(),
    request.getSupplierId(),
    request.getPurchaseDate(),
    request.getQuantity(),
    request.getUnitCost()
);
        /*if (request.getFlockId() != null) {
            purchase.setFlockId(request.getFlockId());
        }*/
        purchase.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(bradaPurchaseRepository.save(purchase));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<BradaPurchase>> getPurchases(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(bradaPurchaseRepository.findByFlockIdAndPurchaseDateBetween(flockId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(bradaPurchaseRepository.findByPurchaseDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(bradaPurchaseRepository.findAll());
    }

    @GetMapping("/summary")
    public ResponseEntity<BradaSummary> getSummary(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        List<BradaPurchase> purchases;
        if (startDate != null && endDate != null) {
            purchases = bradaPurchaseRepository.findByPurchaseDateBetween(startDate, endDate);
        } else {
            purchases = bradaPurchaseRepository.findAll();
        }

        java.math.BigDecimal totalCost = purchases.stream()
            .map(BradaPurchase::getTotalCost)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return ResponseEntity.ok(new BradaSummary(purchases.size(), totalCost));
    }
}

class BradaPurchaseRequest {
    private UUID flockId;
    private UUID supplierId;
    private LocalDate purchaseDate;
    private Integer quantity;
    private java.math.BigDecimal unitCost;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(java.math.BigDecimal unitCost) { this.unitCost = unitCost; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class BradaSummary {
    private int totalPurchases;
    private java.math.BigDecimal totalCost;

    public BradaSummary(int totalPurchases, java.math.BigDecimal totalCost) {
        this.totalPurchases = totalPurchases;
        this.totalCost = totalCost;
    }

    public int getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }
    public java.math.BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(java.math.BigDecimal totalCost) { this.totalCost = totalCost; }
}