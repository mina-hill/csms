package com.csms.csms.controller;

import com.csms.csms.entity.Medicine;
import com.csms.csms.entity.MedicinePurchase;
import com.csms.csms.entity.MedicineUsage;
import com.csms.csms.repository.MedicineRepository;
import com.csms.csms.repository.MedicinePurchaseRepository;
import com.csms.csms.repository.MedicineUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicine")
@CrossOrigin(origins = "*")
public class MedController {

    @Autowired
    private MedicineRepository medicineRepository;
    @Autowired
    private MedicinePurchaseRepository medicinePurchaseRepository;
    @Autowired
    private MedicineUsageRepository medicineUsageRepository;

    // ===== MEDICINES =====

    @GetMapping("/types")
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineRepository.findAll());
    }

    @GetMapping("/types/low-stock")
    public ResponseEntity<List<Medicine>> getLowStockMedicines(@RequestParam(defaultValue = "3") Integer threshold) {
        return ResponseEntity.ok(medicineRepository.findByCurrentStockLessThan(threshold));
    }

    // ===== PURCHASES =====

    @PostMapping("/purchases")
    public ResponseEntity<MedicinePurchase> createPurchase(@RequestBody MedicinePurchaseRequest request) {
        MedicinePurchase purchase = new MedicinePurchase(
            request.getMedicineId(),
            request.getSupplierId(),
            request.getPurchaseDate(),
            request.getQuantity(),
            request.getUnitCost()
        );
        purchase.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(medicinePurchaseRepository.save(purchase));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<MedicinePurchase>> getPurchases(
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (medicineId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(medicinePurchaseRepository.findByMedicineIdAndPurchaseDateBetween(medicineId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(medicinePurchaseRepository.findByPurchaseDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(medicinePurchaseRepository.findAll());
    }

    // ===== USAGE =====

    @PostMapping("/usage")
    public ResponseEntity<MedicineUsage> createUsage(@RequestBody MedicineUsageRequest request) {
        MedicineUsage usage = new MedicineUsage(
            request.getFlockId(),
            request.getMedicineId(),
            request.getRecordDate(),
            request.getDosage()
        );
        usage.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(medicineUsageRepository.save(usage));
    }

    @GetMapping("/usage")
    public ResponseEntity<List<MedicineUsage>> getUsage(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(medicineUsageRepository.findByFlockIdAndUsageDateBetween(flockId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(medicineUsageRepository.findByUsageDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(medicineUsageRepository.findAll());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Medicine>> getStockReport() {
        return ResponseEntity.ok(medicineRepository.findAll());
    }
}

class MedicinePurchaseRequest {
    private UUID medicineId;
    private UUID supplierId;
    private LocalDate purchaseDate;
    private Integer quantity;
    private java.math.BigDecimal unitCost;
    private UUID recordedBy;

    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }
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

class MedicineUsageRequest {
    private UUID flockId;
    private UUID medicineId;
    private LocalDate recordDate;
    private java.math.BigDecimal dosage;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public java.math.BigDecimal getDosage() { return dosage; }
    public void setDosage(java.math.BigDecimal dosage) { this.dosage = dosage; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}