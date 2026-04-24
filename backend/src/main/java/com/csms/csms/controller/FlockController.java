package com.csms.csms.controller;

import com.csms.csms.entity.AuditLog;
import com.csms.csms.entity.Flock;
import com.csms.csms.entity.FlockStatus;
import com.csms.csms.repository.AuditLogRepository;
import com.csms.csms.repository.FlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@RestController
@RequestMapping("/api/flocks")
@CrossOrigin(origins = "*")
public class FlockController {

    @Autowired private FlockRepository flockRepository;
    @Autowired
    @Qualifier("auditLogRepository")
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<Flock>> getAllFlocks() {
        return ResponseEntity.ok(flockRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Flock>> getActiveFlocks() {
        return ResponseEntity.ok(flockRepository.findByStatus(FlockStatus.ACTIVE));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flock> getFlockById(@PathVariable UUID id) {
        return flockRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Flock-scoped events from unified {@code audit_log} (newest first). */
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLog>> getFlockAudit(@PathVariable UUID id) {
        return ResponseEntity.ok(
                auditLogRepository.findByFlockIdOrderByLoggedAtDesc(id));
    }

    @PostMapping
    public ResponseEntity<?> registerFlock(@Valid @RequestBody FlockRequest req) {
        String code = generateNextFlockCode();

        Flock flock = new Flock(
                code,
                req.getBreed(),
                req.getInitialQty(),
                req.getArrivalDate(),
                req.getSupplierId(),
                req.getNotes(),
                req.getCreatedBy()
        );

        Flock saved = flockRepository.save(flock);

        auditLogRepository.save(new AuditLog(
                saved.getCreatedBy(),
                "FLOCK_CREATED",
                "flocks",
                saved.getFlockId(),
                saved.getFlockId(),
                "{\"breed\":\"" + saved.getBreed() +
                        "\",\"initial_qty\":" + saved.getInitialQty() + "}"
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFlock(@PathVariable UUID id,
                                         @RequestBody FlockRequest req) {
        Optional<Flock> existing = flockRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Flock flock = existing.get();

        if (flock.getStatus() == FlockStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Flock is CLOSED and cannot be updated."));
        }

        String oldValues = "{\"breed\":\"" + flock.getBreed() +
                "\",\"current_qty\":" + flock.getCurrentQty() +
                ",\"notes\":\"" + (flock.getNotes() != null ? flock.getNotes() : "") + "\"}";

        if (req.getBreed()      != null) flock.setBreed(req.getBreed());
        if (req.getCurrentQty() != null) flock.setCurrentQty(req.getCurrentQty());
        if (req.getNotes()      != null) flock.setNotes(req.getNotes());

        Flock updated = flockRepository.save(flock);

        String newValues = "{\"breed\":\"" + updated.getBreed() +
                "\",\"current_qty\":" + updated.getCurrentQty() +
                ",\"notes\":\"" + (updated.getNotes() != null ? updated.getNotes() : "") + "\"}";

        String details = "{\"old_values\":" + oldValues + ",\"new_values\":" + newValues + "}";
        auditLogRepository.save(new AuditLog(
                req.getUpdatedBy(),
                "FLOCK_UPDATED",
                "flocks",
                updated.getFlockId(),
                updated.getFlockId(),
                details
        ));

        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<?> closeFlock(@PathVariable UUID id,
                                        @RequestBody CloseFlockRequest req) {
        Optional<Flock> existing = flockRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Flock flock = existing.get();

        if (flock.getStatus() == FlockStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Flock is already CLOSED."));
        }

        String oldValues = "{\"status\":\"ACTIVE\"}";

        LocalDate closeDate = req.getCloseDate() != null ? req.getCloseDate() : LocalDate.now();
        flock.setStatus(FlockStatus.CLOSED);
        flock.setCloseDate(closeDate);
        flock.setClosedBy(req.getClosedBy());

        Flock closed = flockRepository.save(flock);

        String newValues = "{\"status\":\"CLOSED\",\"close_date\":\"" + closeDate + "\"}";
        String details = "{\"old_values\":" + oldValues + ",\"new_values\":" + newValues + "}";
        auditLogRepository.save(new AuditLog(
                null,
                "FLOCK_CLOSED",
                "flocks",
                closed.getFlockId(),
                closed.getFlockId(),
                details
        ));

        return ResponseEntity.ok(closed);
    }

    private String generateNextFlockCode() {
        long count = flockRepository.count();
        String candidate;
        do {
            count++;
            candidate = String.format("FLK-%03d", count);
        } while (flockRepository.existsByFlockCode(candidate));
        return candidate;
    }
}

class FlockRequest {

    @NotBlank(message = "Breed is required")
    private String breed;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 1, message = "Initial quantity must be greater than 0")
    private Integer initialQty;

    private Integer currentQty;

    @NotNull(message = "Arrival date is required")
    private LocalDate arrivalDate;

    private UUID supplierId;
    private String notes;
    private UUID createdBy;
    private UUID updatedBy;

    public FlockRequest() {}

    public String getBreed() { return breed; }
    public void setBreed(String v) { this.breed = v; }

    public Integer getInitialQty() { return initialQty; }
    public void setInitialQty(Integer v) { this.initialQty = v; }

    public Integer getCurrentQty() { return currentQty; }
    public void setCurrentQty(Integer v) { this.currentQty = v; }

    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDate v) { this.arrivalDate = v; }

    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID v) { this.supplierId = v; }

    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
}

class CloseFlockRequest {
    private LocalDate closeDate;
    private String    closedBy;

    public CloseFlockRequest() {}

    public LocalDate getCloseDate()            { return closeDate; }
    public void      setCloseDate(LocalDate v) { this.closeDate = v; }

    public String    getClosedBy()             { return closedBy; }
    public void      setClosedBy(String v)     { this.closedBy = v; }
}

class ErrorResponse {
    private String message;
    public ErrorResponse(String m) { this.message = m; }
    public String getMessage()     { return message; }
    public void setMessage(String m) { this.message = m; }
    @Override
public String toString() {
    return message; // or whatever your field is named
}
}

