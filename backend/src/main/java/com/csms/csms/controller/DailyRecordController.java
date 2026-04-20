package com.csms.csms.controller;

import com.csms.csms.entity.AuditLog;
import com.csms.csms.entity.DailyMortality;
import com.csms.csms.entity.FlockStatus;
import com.csms.csms.entity.WeeklyWeight;
import com.csms.csms.repository.AuditLogRepository;
import com.csms.csms.repository.DailyMortalityRepository;
import com.csms.csms.repository.FlockRepository;
import com.csms.csms.repository.WeeklyWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DailyRecordController
 * ─────────────────────
 * POST /api/mortality            — US-004: record one shift's mortality
 * GET  /api/mortality/{flockId}  — all mortality records for a flock
 * POST /api/weights              — US-005: record weekly avg weight
 * GET  /api/weights/{flockId}    — all weight records for a flock (asc by week)
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DailyRecordController {

    @Autowired
    private DailyMortalityRepository mortalityRepository;

    @Autowired
    private WeeklyWeightRepository weightRepository;

    @Autowired
    private FlockRepository flockRepository;

    // Use @Qualifier to disambiguate from FlockController's FlockAuditLogRepository
    // (both fields are named "auditLogRepository" in their respective controllers,
    //  but Spring resolves by type — AuditLogRepository vs FlockAuditLogRepository
    //  are different types so there is no actual conflict; the @Qualifier is
    //  included as an explicit safety net)
    @Autowired
    @Qualifier("auditLogRepository")
    private AuditLogRepository systemAuditLog;

    // ── MORTALITY ─────────────────────────────────────────────────────────────

    /** GET /api/mortality — all records (for testing / dashboard) */
    @GetMapping("/mortality")
    public ResponseEntity<List<DailyMortality>> getAllMortality() {
        return ResponseEntity.ok(mortalityRepository.findAll());
    }

    /** GET /api/mortality/{flockId} — records for one flock */
    @GetMapping("/mortality/{flockId}")
    public ResponseEntity<List<DailyMortality>> getMortalityByFlock(
            @PathVariable UUID flockId) {
        return ResponseEntity.ok(mortalityRepository.findByFlockId(flockId));
    }

    /**
     * POST /api/mortality
     *
     * Pre-checks mirror the DB BEFORE INSERT trigger:
     *  - flock must be ACTIVE
     *  - (flock_id, record_date, shift) must be unique
     * The trigger itself is the final guard; we pre-check to return clean 409s.
     */
    @PostMapping("/mortality")
    public ResponseEntity<?> recordMortality(@RequestBody MortalityRequest req) {

        if (req.getFlockId() == null || req.getRecordDate() == null
                || req.getCount() == null || req.getShift() == null) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("flockId, recordDate, count, and shift are required."));
        }

        String shift = req.getShift().toUpperCase();
        if (!shift.equals("DAY") && !shift.equals("NIGHT")) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("shift must be DAY or NIGHT."));
        }

        if (req.getCount() < 0) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("count cannot be negative."));
        }

        var flockOpt = flockRepository.findById(req.getFlockId());
        if (flockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (flockOpt.get().getStatus() == FlockStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DailyRecordError("Cannot record mortality for a CLOSED flock."));
        }

        Optional<DailyMortality> duplicate = mortalityRepository
                .findByFlockIdAndRecordDateAndShift(
                        req.getFlockId(), req.getRecordDate(), shift);
        if (duplicate.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DailyRecordError(shift + " mortality already recorded for "
                            + req.getRecordDate() + " on this flock."));
        }

        DailyMortality record = new DailyMortality(
                req.getFlockId(),
                req.getRecordDate(),
                req.getCount(),
                shift,
                req.getRecordedBy()
        );
        DailyMortality saved = mortalityRepository.save(record);

        systemAuditLog.save(new AuditLog(
                req.getRecordedBy(),
                "MORTALITY_RECORDED",
                "daily_mortality",
                saved.getMortalityId(),
                "{\"flock_id\":\"" + req.getFlockId()
                        + "\",\"date\":\"" + req.getRecordDate()
                        + "\",\"shift\":\"" + shift
                        + "\",\"count\":" + req.getCount() + "}"
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── WEEKLY WEIGHT ─────────────────────────────────────────────────────────

    /** GET /api/weights — all records (for testing / dashboard) */
    @GetMapping("/weights")
    public ResponseEntity<List<WeeklyWeight>> getAllWeights() {
        return ResponseEntity.ok(weightRepository.findAll());
    }

    /** GET /api/weights/{flockId} — records for one flock */
    @GetMapping("/weights/{flockId}")
    public ResponseEntity<List<WeeklyWeight>> getWeightsByFlock(
            @PathVariable UUID flockId) {
        return ResponseEntity.ok(
                weightRepository.findByFlockIdOrderByWeekNumber(flockId));
    }

    /**
     * POST /api/weights
     *
     * Pre-checks mirror the DB constraints:
     *  - flock must be ACTIVE
     *  - (flock_id, week_number) must be unique
     *  - record_date must not be in the future
     */
    @PostMapping("/weights")
    public ResponseEntity<?> recordWeight(@RequestBody WeightRequest req) {

        if (req.getFlockId() == null || req.getWeekNumber() == null
                || req.getRecordDate() == null || req.getAvgWeightKg() == null) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError(
                            "flockId, weekNumber, recordDate, and avgWeightKg are required."));
        }

        if (req.getWeekNumber() < 1) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("weekNumber must be >= 1."));
        }

        if (req.getAvgWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("avgWeightKg must be > 0."));
        }

        if (req.getRecordDate().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(new DailyRecordError("recordDate cannot be in the future."));
        }

        var flockOpt = flockRepository.findById(req.getFlockId());
        if (flockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (flockOpt.get().getStatus() == FlockStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DailyRecordError("Cannot record weight for a CLOSED flock."));
        }

        Optional<WeeklyWeight> duplicate = weightRepository
                .findByFlockIdAndWeekNumber(req.getFlockId(), req.getWeekNumber());
        if (duplicate.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DailyRecordError("Week " + req.getWeekNumber()
                            + " already recorded for this flock."));
        }

        WeeklyWeight record = new WeeklyWeight(
                req.getFlockId(),
                req.getWeekNumber(),
                req.getRecordDate(),
                req.getAvgWeightKg(),
                req.getRecordedBy()
        );
        WeeklyWeight saved = weightRepository.save(record);

        systemAuditLog.save(new AuditLog(
                req.getRecordedBy(),
                "WEIGHT_RECORDED",
                "weekly_weight",
                saved.getWeightId(),
                "{\"flock_id\":\"" + req.getFlockId()
                        + "\",\"week_number\":" + req.getWeekNumber()
                        + ",\"avg_weight_kg\":" + req.getAvgWeightKg() + "}"
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

class MortalityRequest {
    private UUID      flockId;
    private LocalDate recordDate;
    private Integer   count;
    private String    shift;
    private UUID      recordedBy;

    public MortalityRequest() {}

    public UUID      getFlockId()              { return flockId; }
    public void      setFlockId(UUID v)        { this.flockId = v; }

    public LocalDate getRecordDate()           { return recordDate; }
    public void      setRecordDate(LocalDate v){ this.recordDate = v; }

    public Integer   getCount()               { return count; }
    public void      setCount(Integer v)      { this.count = v; }

    public String    getShift()               { return shift; }
    public void      setShift(String v)       { this.shift = v; }

    public UUID      getRecordedBy()          { return recordedBy; }
    public void      setRecordedBy(UUID v)    { this.recordedBy = v; }
}

class WeightRequest {
    private UUID       flockId;
    private Integer    weekNumber;
    private LocalDate  recordDate;
    private BigDecimal avgWeightKg;
    private UUID       recordedBy;

    public WeightRequest() {}

    public UUID       getFlockId()                { return flockId; }
    public void       setFlockId(UUID v)          { this.flockId = v; }

    public Integer    getWeekNumber()             { return weekNumber; }
    public void       setWeekNumber(Integer v)    { this.weekNumber = v; }

    public LocalDate  getRecordDate()             { return recordDate; }
    public void       setRecordDate(LocalDate v)  { this.recordDate = v; }

    public BigDecimal getAvgWeightKg()            { return avgWeightKg; }
    public void       setAvgWeightKg(BigDecimal v){ this.avgWeightKg = v; }

    public UUID       getRecordedBy()             { return recordedBy; }
    public void       setRecordedBy(UUID v)       { this.recordedBy = v; }
}

class DailyRecordError {
    private String message;
    public DailyRecordError(String m)    { this.message = m; }
    public String getMessage()           { return message; }
    public void setMessage(String m)     { this.message = m; }
}