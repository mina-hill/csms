package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.Medicine;
import com.csms.csms.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicines")
@CrossOrigin(origins = "*")
public class MedicineController {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Medicine>> getLowStockMedicines(@RequestParam(required = false) Integer threshold) {
        if (threshold != null) {
            return ResponseEntity.ok(medicineRepository.findByCurrentStockLessThan(threshold));
        }
        return ResponseEntity.ok(medicineRepository.findByCurrentStockLessThanMinThreshold());
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsertMedicine(
            @RequestBody MedicineUpsertRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("name is required.");
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
        return ResponseEntity.status(existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/threshold")
    public ResponseEntity<?> updateThreshold(
            @PathVariable UUID id,
            @RequestBody MedicineThresholdRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getMinThreshold() == null || request.getMinThreshold() < 0) {
            return ResponseEntity.badRequest().body("minThreshold must be >= 0.");
        }
        Optional<Medicine> existing = medicineRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Medicine medicine = existing.get();
        medicine.setMinThreshold(request.getMinThreshold());
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @PatchMapping("/stock")
    public ResponseEntity<?> adjustStock(
            @RequestBody MedicineStockAdjustRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("name is required.");
        }
        if (request.getDelta() == null || request.getDelta() == 0) {
            return ResponseEntity.badRequest().body("delta must be non-zero.");
        }
        String normalizedName = request.getName().trim();
        Medicine medicine = medicineRepository.findByName(normalizedName).orElse(null);
        if (medicine == null) {
            if (request.getDelta() < 0) {
                return ResponseEntity.badRequest().body("Cannot decrement stock for non-existing medicine.");
            }
            medicine = new Medicine(normalizedName, request.getMinThreshold() != null ? request.getMinThreshold() : 5);
            medicine.setCurrentStock(0);
        }
        int nextStock = (medicine.getCurrentStock() == null ? 0 : medicine.getCurrentStock()) + request.getDelta();
        if (nextStock < 0) {
            return ResponseEntity.badRequest().body("Insufficient medicine stock.");
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
}

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
