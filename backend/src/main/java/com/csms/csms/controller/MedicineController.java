package com.csms.csms.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.csms.csms.auth.CsmsAccessHelper;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class MedicineController {

    @Autowired private MedicineRepository medicineRepository;
    @Autowired private MedicinePurchaseRepository medicinePurchaseRepository;
    @Autowired private MedicineUsageRepository medicineUsageRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private CsmsAccessHelper accessHelper;

    // ===== CORE MEDICINE OPERATIONS (/api/medicines) =====

    @GetMapping("/api/medicines")
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/api/medicines/low-stock")
    public ResponseEntity<List<Medicine>> getLowStockMedicines(
            @RequestParam(required = false) Integer threshold) {
        if (threshold != null) {
            return ResponseEntity.ok(medicineRepository.findByCurrentStockLessThan(threshold));
        }
        return ResponseEntity.ok(medicineRepository.findByCurrentStockLessThanMinThreshold());
    }

    @PostMapping("/api/medicines/upsert")
    public ResponseEntity<?> upsertMedicine(
            @RequestBody MedicineUpsertRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required."));
        }
        String normalizedName = request.getName().trim();
        int threshold = request.getMinThreshold() != null ? request.getMinThreshold() : 5;
        if (threshold < 0) threshold = 0;
        final int normalizedThreshold = threshold;

        Optional<Medicine> existing = medicineRepository.findByName(normalizedName);
        Medicine medicine = existing.orElseGet(() -> new Medicine(normalizedName, normalizedThreshold));
        if (existing.isPresent()) {
            medicine.setMinThreshold(normalizedThreshold);
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            medicine.setUnit(request.getUnit().trim());
        }
        Medicine saved = medicineRepository.save(medicine);
        return ResponseEntity.status(existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(saved);
    }

    @PatchMapping("/api/medicines/{id}/threshold")
    public ResponseEntity<?> updateThreshold(
            @PathVariable UUID id,
            @RequestBody MedicineThresholdRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getMinThreshold() == null || request.getMinThreshold() < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "minThreshold must be >= 0."));
        }
        Optional<Medicine> existing = medicineRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Medicine medicine = existing.get();
        medicine.setMinThreshold(request.getMinThreshold());
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @PatchMapping("/api/medicines/stock")
    public ResponseEntity<?> adjustStock(
            @RequestBody MedicineStockAdjustRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required."));
        }
        if (request.getDelta() == null || request.getDelta() == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "delta must be non-zero."));
        }
        String normalizedName = request.getName().trim();
        Medicine medicine = medicineRepository.findByName(normalizedName).orElse(null);
        if (medicine == null) {
            if (request.getDelta() < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot decrement stock for non-existing medicine."));
            }
            medicine = new Medicine(normalizedName,
                    request.getMinThreshold() != null ? request.getMinThreshold() : 5);
            medicine.setCurrentStock(0);
        }
        int nextStock = (medicine.getCurrentStock() == null ? 0 : medicine.getCurrentStock())
                + request.getDelta();
        if (nextStock < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Insufficient medicine stock."));
        }
        medicine.setCurrentStock(nextStock);
        if (request.getMinThreshold() != null && request.getMinThreshold() >= 0) {
            medicine.setMinThreshold(request.getMinThreshold());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            medicine.setUnit(request.getUnit().trim());
        }
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    // ===== MEDICINE PURCHASES (/api/medicine/purchases) =====

    @PostMapping("/api/medicine/purchases")
    public ResponseEntity<?> createPurchase(@RequestBody MedicinePurchaseRequest request) {
        UUID medicineId = request.getMedicineId();
        if (medicineId == null && request.getMedicineName() != null
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
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity must be a positive integer"));
        }
        if (request.getUnitCost() == null || request.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "unitCost must be positive"));
        }
        if (medicineRepository.findById(medicineId).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Medicine not found for id: " + medicineId));
        }
        if (supplierRepository.findById(request.getSupplierId()).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Supplier not found for id: " + request.getSupplierId()));
        }

        MedicinePurchase purchase = new MedicinePurchase(
            medicineId,
            request.getSupplierId(),
            request.getPurchaseDate(),
            request.getQuantity(),
            request.getUnitCost()
        );
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            String unit = request.getUnit().trim();
            if (unit.length() > 60) unit = unit.substring(0, 60);
            purchase.setUnit(unit);
        }
        purchase.setRecordedBy(request.getRecordedBy());
        try {
            medicinePurchaseRepository.save(purchase);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "recorded"));
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

    @GetMapping("/api/medicine/purchases")
    public ResponseEntity<List<MedicinePurchase>> getPurchases(
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if (medicineId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(medicinePurchaseRepository
                    .findByMedicineIdAndPurchaseDateBetween(medicineId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(medicinePurchaseRepository
                    .findByPurchaseDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(medicinePurchaseRepository.findAll());
    }

    // ===== MEDICINE USAGE (/api/medicine/usage) =====

    @PostMapping("/api/medicine/usage")
    public ResponseEntity<?> createUsage(@RequestBody MedicineUsageRequest request) {
        if (request.getFlockId() == null || request.getMedicineId() == null
                || request.getUsageDate() == null || request.getDosage() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "flockId, medicineId, usageDate, and dosage are required"));
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
            request.getUsageDate(),
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
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(medicineUsageRepository.save(usage));
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

    @GetMapping("/api/medicine/usage")
    public ResponseEntity<List<MedicineUsage>> getUsage(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(medicineUsageRepository
                    .findByFlockIdAndUsageDateBetween(flockId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(medicineUsageRepository
                    .findByUsageDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(medicineUsageRepository.findAll());
    }
}

// ===== REQUEST DTOs =====

class MedicineUpsertRequest {
    private String name;
    private Integer minThreshold;
    private String unit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}

class MedicineThresholdRequest {
    private Integer minThreshold;

    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
}

class MedicineStockAdjustRequest {
    private String name;
    private Integer delta;
    private Integer minThreshold;
    private String unit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}

class MedicinePurchaseRequest {
    private UUID medicineId;
    private String medicineName;
    private UUID supplierId;
    private LocalDate purchaseDate;
    private Integer quantity;
    private BigDecimal unitCost;
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
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class MedicineUsageRequest {
    private UUID flockId;
    private UUID medicineId;
    @JsonAlias("usage_date")
    private LocalDate usageDate;
    private BigDecimal dosage;
    private String unit;
    @JsonAlias("usage_time")
    private String usageTime;
    private String notes;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }
    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }
    public BigDecimal getDosage() { return dosage; }
    public void setDosage(BigDecimal dosage) { this.dosage = dosage; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getUsageTime() { return usageTime; }
    public void setUsageTime(String usageTime) { this.usageTime = usageTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}