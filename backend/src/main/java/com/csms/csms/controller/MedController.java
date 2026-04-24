package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.csms.csms.entity.Medicine;
import com.csms.csms.entity.MedicinePurchase;
import com.csms.csms.entity.MedicineUsage;
import com.csms.csms.repository.MedicinePurchaseRepository;
import com.csms.csms.repository.MedicineRepository;
import com.csms.csms.repository.MedicineUsageRepository;
import com.csms.csms.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

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
    public ResponseEntity<?> createPurchase(
            @RequestBody MedicinePurchaseRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireFinancialOrThrow(actorId);
        UUID medicineId = request.getMedicineId();
        if (medicineId == null
                && request.getMedicineName() != null
                && !request.getMedicineName().isBlank()) {
            medicineId = medicineRepository.findByName(request.getMedicineName().trim())
                    .map(Medicine::getMedicineId)
                    .orElse(null);
        }
        if (medicineId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "Provide medicineId or medicineName (trimmed) that matches an existing medicine."));
        }
        if (request.getSupplierId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "supplierId is required"));
        }
        if (request.getPurchaseDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "purchaseDate is required"));
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "quantity must be a positive integer"));
        }
        if (request.getUnitCost() == null || request.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "unitCost must be positive"));
        }
        if (medicineRepository.findById(medicineId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Medicine not found for id: " + medicineId));
        }
        if (supplierRepository.findById(request.getSupplierId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Supplier not found for id: " + request.getSupplierId()));
        }

        MedicinePurchase purchase = new MedicinePurchase(
            medicineId,
            request.getSupplierId(),
            request.getPurchaseDate(),
            request.getQuantity(),
            request.getUnitCost()
        );
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            String u = request.getUnit().trim();
            if (u.length() > 60) {
                u = u.substring(0, 60);
            }
            purchase.setUnit(u);
        }
        purchase.setRecordedBy(request.getRecordedBy());
        try {
            medicinePurchaseRepository.save(purchase);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "recorded"
            ));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Could not save medicine purchase (check foreign keys and unique constraints).",
                    "detail", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
            ));
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Database error while saving medicine purchase.",
                    "detail", cause != null ? cause.getMessage() : e.getMessage()
            ));
        }
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
    public ResponseEntity<?> createUsage(
            @RequestBody MedicineUsageRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireFinancialOrThrow(actorId);
        if (request.getFlockId() == null || request.getMedicineId() == null
                || request.getRecordDate() == null || request.getDosage() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "flockId, medicineId, recordDate, and dosage are required"));
        }
        if (request.getDosage().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "dosage must be positive"));
        }
        if (request.getUsageTime() == null || request.getUsageTime().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "usageTime is required"));
        }

        MedicineUsage usage = new MedicineUsage(
            request.getFlockId(),
            request.getMedicineId(),
            request.getRecordDate(),
            request.getDosage()
        );
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            usage.setUnit(request.getUnit().trim());
        }
        usage.setUsageTime(request.getUsageTime().trim());
        if (request.getNotes() != null) {
            usage.setNotes(request.getNotes());
        }
        usage.setRecordedBy(request.getRecordedBy());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(medicineUsageRepository.save(usage));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Could not save medicine usage.",
                    "detail", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
            ));
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Database error while saving medicine usage.",
                    "detail", cause != null ? cause.getMessage() : e.getMessage()
            ));
        }
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
    /** Resolved server-side with the same trimmed name lookup as PATCH /api/medicines/stock when medicineId is omitted. */
    private String medicineName;
    private UUID supplierId;
    private LocalDate purchaseDate;
    private Integer quantity;
    private java.math.BigDecimal unitCost;
    private String unit;
    private UUID recordedBy;

    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(java.math.BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class MedicineUsageRequest {
    private UUID flockId;
    private UUID medicineId;
    private LocalDate recordDate;
    private java.math.BigDecimal dosage;
    private String unit;
    @JsonAlias("usage_time")
    private String usageTime;
    private String notes;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public java.math.BigDecimal getDosage() { return dosage; }
    public void setDosage(java.math.BigDecimal dosage) { this.dosage = dosage; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getUsageTime() { return usageTime; }
    public void setUsageTime(String usageTime) { this.usageTime = usageTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}