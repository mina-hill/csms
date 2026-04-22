package com.csms.csms.controller;

import com.csms.csms.entity.AuditLog;
import com.csms.csms.entity.DailyMortality;
import com.csms.csms.entity.WeeklySummary;
import com.csms.csms.repository.AuditLogRepository;
import com.csms.csms.repository.DailyMortalityRepository;
import com.csms.csms.repository.FlockRepository;
import com.csms.csms.repository.WeeklyWeightRepository;
import com.csms.csms.repository.WeeklySummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WeeklySummaryController
 * ───────────────────────
 * POST /api/weekly-summary            — create or update a snapshot
 * GET  /api/weekly-summary/{flockId}  — all snapshots for a flock (asc by week)
 *
 * The controller derives/aggregates everything it can from the DB:
 *   chick_age_days       — computed from flock.arrival_date
 *   total_chicks         — flock.initial_qty
 *   remaining_chicks     — flock.current_qty at save time
 *   weekly_mortality     — queried from daily_mortality for [weekEnd-6d .. weekEnd]
 *   cumulative_mortality — queried from daily_mortality for all time
 *   avg_weight_kg        — looked up from weekly_weight for the same week_number
 *   fcr_value            — computed: (cumFeedSacks*50) / (avgWeightKg * remaining)
 *
 * Feed sack counts are passed in by the caller because feed_usage is a separate
 * domain managed by a different controller.
 */
@RestController
@RequestMapping("/api/weekly-summary")
@CrossOrigin(origins = "*")
public class WeeklySummaryController {

    @Autowired
    private WeeklySummaryRepository summaryRepository;

    @Autowired
    private FlockRepository flockRepository;

    @Autowired
    private DailyMortalityRepository mortalityRepository;

    @Autowired
    private WeeklyWeightRepository weightRepository;

    @Autowired
    @Qualifier("auditLogRepository")
    private AuditLogRepository systemAuditLog;

    // ── GET ───────────────────────────────────────────────────────────────────

    /** GET /api/weekly-summary — all snapshots (for testing / dashboard) */
    @GetMapping
    public ResponseEntity<List<WeeklySummary>> getAllSummaries() {
        return ResponseEntity.ok(summaryRepository.findAll());
    }

    /** GET /api/weekly-summary/{flockId} — snapshots for one flock */
    @GetMapping("/{flockId}")
    public ResponseEntity<List<WeeklySummary>> getSummariesByFlock(
            @PathVariable UUID flockId) {
        return ResponseEntity.ok(
                summaryRepository.findByFlockIdOrderByWeekNumber(flockId));
    }

    // ── POST (upsert) ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> saveWeeklySummary(@RequestBody WeeklySummaryRequest req) {

        if (req.getFlockId() == null || req.getWeekNumber() == null
                || req.getWeekEndDate() == null) {
            return ResponseEntity.badRequest()
                    .body(new WeeklySummaryError(
                            "flockId, weekNumber, and weekEndDate are required."));
        }

        var flockOpt = flockRepository.findById(req.getFlockId());
        if (flockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var flock = flockOpt.get();

        // ── derived fields ────────────────────────────────────────────────────
        int chickAgeDays = (int) ChronoUnit.DAYS.between(
                flock.getArrivalDate(), req.getWeekEndDate());

        // ── weekly mortality: sum count for the 7-day window ──────────────────
        LocalDate weekStart = req.getWeekEndDate().minusDays(6);
        List<DailyMortality> weekMortRecords =
                mortalityRepository.findByFlockIdAndRecordDateBetween(
                        req.getFlockId(), weekStart, req.getWeekEndDate());
        int weeklyMortality = weekMortRecords.stream()
                .mapToInt(m -> m.getCount() != null ? m.getCount() : 0)
                .sum();

        // ── cumulative mortality: all records for this flock ──────────────────
        int cumulativeMortality = mortalityRepository
                .findByFlockId(req.getFlockId())
                .stream()
                .mapToInt(m -> m.getCount() != null ? m.getCount() : 0)
                .sum();

        // ── avg_weight_kg: from weekly_weight for same week_number ────────────
        BigDecimal avgWeightKg = null;
        // WeeklyWeight is imported at the top of this file
        Optional<com.csms.csms.entity.WeeklyWeight> weightOpt =
                weightRepository.findByFlockIdAndWeekNumber(

                        req.getFlockId(), req.getWeekNumber());
        if (weightOpt.isPresent()) {
            avgWeightKg = weightOpt.get().getAvgWeightKg();
        }

        // ── FCR: (cumFeedSacks * 50) / (avgWeightKg * remainingChicks) ────────
        int remainingChicks  = flock.getCurrentQty();
        int cumulativeFeedSacks = req.getCumulativeFeedSacks() != null
                ? req.getCumulativeFeedSacks() : 0;

        BigDecimal fcrValue = null;
        if (avgWeightKg != null
                && avgWeightKg.compareTo(BigDecimal.ZERO) > 0
                && remainingChicks > 0
                && cumulativeFeedSacks > 0) {
            BigDecimal numerator   = BigDecimal.valueOf(cumulativeFeedSacks * 50L);
            BigDecimal denominator = avgWeightKg.multiply(
                    BigDecimal.valueOf(remainingChicks));
            fcrValue = numerator.divide(denominator, 3, RoundingMode.HALF_UP);
        }

        // ── upsert ────────────────────────────────────────────────────────────
        Optional<WeeklySummary> existing =
                summaryRepository.findByFlockIdAndWeekNumber(
                        req.getFlockId(), req.getWeekNumber());

        WeeklySummary summary = existing.orElse(new WeeklySummary());
        summary.setFlockId(req.getFlockId());
        summary.setWeekNumber(req.getWeekNumber());
        summary.setWeekEndDate(req.getWeekEndDate());
        summary.setChickAgeDays(chickAgeDays);
        summary.setTotalChicks(flock.getInitialQty());
        summary.setRemainingChicks(remainingChicks);
        summary.setWeeklyMortality(weeklyMortality);
        summary.setCumulativeMortality(cumulativeMortality);
        summary.setAvgWeightKg(avgWeightKg);
        summary.setWeeklyFeedSacks(
                req.getWeeklyFeedSacks() != null ? req.getWeeklyFeedSacks() : 0);
        summary.setCumulativeFeedSacks(cumulativeFeedSacks);
        summary.setFcrValue(fcrValue);
        summary.setRecordedBy(req.getRecordedBy());

        WeeklySummary saved = summaryRepository.save(summary);

        systemAuditLog.save(new AuditLog(
                req.getRecordedBy(),
                existing.isPresent() ? "WEEKLY_SUMMARY_UPDATED" : "WEEKLY_SUMMARY_CREATED",
                "weekly_summary",
                saved.getSummaryId(),
                req.getFlockId(),
                "{\"flock_id\":\"" + req.getFlockId()
                        + "\",\"week_number\":" + req.getWeekNumber()
                        + ",\"fcr_value\":" + (fcrValue != null ? fcrValue : "null") + "}"
        ));

        return ResponseEntity.status(
                existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED
        ).body(saved);
    }
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

class WeeklySummaryRequest {
    private UUID      flockId;
    private Integer   weekNumber;
    private LocalDate weekEndDate;
    private Integer   weeklyFeedSacks;
    private Integer   cumulativeFeedSacks;
    private UUID      recordedBy;

    public WeeklySummaryRequest() {}

    public UUID      getFlockId()                      { return flockId; }
    public void      setFlockId(UUID v)                { this.flockId = v; }

    public Integer   getWeekNumber()                   { return weekNumber; }
    public void      setWeekNumber(Integer v)          { this.weekNumber = v; }

    public LocalDate getWeekEndDate()                  { return weekEndDate; }
    public void      setWeekEndDate(LocalDate v)       { this.weekEndDate = v; }

    public Integer   getWeeklyFeedSacks()              { return weeklyFeedSacks; }
    public void      setWeeklyFeedSacks(Integer v)     { this.weeklyFeedSacks = v; }

    public Integer   getCumulativeFeedSacks()          { return cumulativeFeedSacks; }
    public void      setCumulativeFeedSacks(Integer v) { this.cumulativeFeedSacks = v; }

    public UUID      getRecordedBy()                   { return recordedBy; }
    public void      setRecordedBy(UUID v)             { this.recordedBy = v; }
}

class WeeklySummaryError {
    private String message;
    public WeeklySummaryError(String m)  { this.message = m; }
    public String getMessage()           { return message; }
    public void setMessage(String m)     { this.message = m; }
}