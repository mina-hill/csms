package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.FeedPurchase;
import com.csms.csms.entity.FeedUsage;
import com.csms.csms.entity.FeedSale;
import com.csms.csms.entity.FeedType;
import com.csms.csms.repository.FeedPurchaseRepository;
import com.csms.csms.repository.FeedUsageRepository;
import com.csms.csms.repository.FeedSaleRepository;
import com.csms.csms.repository.FeedTypeRepository;
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
@RequestMapping("/api/feed")
@CrossOrigin(origins = "*")
public class FeedController {

    @Autowired
    private FeedTypeRepository feedTypeRepository;
    @Autowired
    private FeedPurchaseRepository feedPurchaseRepository;
    @Autowired
    private FeedUsageRepository feedUsageRepository;
    @Autowired
    private FeedSaleRepository feedSaleRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

    // ===== FEED TYPES =====

    @GetMapping("/types")
    public ResponseEntity<List<FeedType>> getAllFeedTypes() {
        return ResponseEntity.ok(feedTypeRepository.findAll());
    }

    @GetMapping("/types/low-stock")
    public ResponseEntity<List<FeedType>> getLowStockFeedTypes(@RequestParam(defaultValue = "5") Integer threshold) {
        return ResponseEntity.ok(feedTypeRepository.findByCurrentStockLessThan(threshold));
    }

    // ===== PURCHASES =====

    @PostMapping("/purchases")
    public ResponseEntity<?> createPurchase(
            @RequestBody FeedPurchaseRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireFinancialOrThrow(actorId);
        if (request.getFeedTypeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "feedTypeId is required"));
        }
        if (request.getSupplierId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "supplierId is required"));
        }
        if (request.getPurchaseDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "purchaseDate is required"));
        }
        if (request.getSacksQty() == null || request.getSacksQty() < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "sacksQty must be a positive integer"));
        }
        if (request.getCostPerSack() == null || request.getCostPerSack().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "costPerSack must be positive"));
        }
        if (feedTypeRepository.findById(request.getFeedTypeId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Feed type not found for id: " + request.getFeedTypeId()));
        }
        if (supplierRepository.findById(request.getSupplierId()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Supplier not found for id: " + request.getSupplierId()));
        }

        FeedPurchase purchase = new FeedPurchase(
            request.getFeedTypeId(),
            request.getSupplierId(),
            request.getPurchaseDate(),
            request.getSacksQty(),
            request.getCostPerSack()
        );
        purchase.setRecordedBy(request.getRecordedBy());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(feedPurchaseRepository.save(purchase));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Could not save feed purchase (check foreign keys and unique constraints).",
                    "detail", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
            ));
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Database error while saving feed purchase.",
                    "detail", cause != null ? cause.getMessage() : e.getMessage()
            ));
        }
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<FeedPurchase>> getPurchases(
            @RequestParam(required = false) UUID feedTypeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (feedTypeId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(feedPurchaseRepository.findByFeedTypeIdAndPurchaseDateBetween(feedTypeId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(feedPurchaseRepository.findByPurchaseDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(feedPurchaseRepository.findAll());
    }

    // ===== USAGE =====

    @PostMapping("/usage")
    public ResponseEntity<?> createUsage(
            @RequestBody FeedUsageRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireFinancialOrThrow(actorId);
        if (request.getFlockId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "flockId is required"));
        }
        if (request.getFeedTypeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "feedTypeId is required"));
        }
        if (request.getRecordDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "recordDate is required"));
        }
        if (request.getSacksUsed() == null || request.getSacksUsed() < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "sacksUsed must be a positive integer"));
        }
        if (request.getShift() == null || request.getShift().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "shift is required (DAY or NIGHT)"));
        }
        String shift = request.getShift().trim().toUpperCase();
        if (!shift.equals("DAY") && !shift.equals("NIGHT")) {
            return ResponseEntity.badRequest().body(Map.of("error", "shift must be DAY or NIGHT"));
        }

        try {
            var existingUsage = feedUsageRepository.findByFlockIdAndFeedTypeIdAndUsageDateAndShift(
                    request.getFlockId(),
                    request.getFeedTypeId(),
                    request.getRecordDate(),
                    shift
            );

            FeedUsage usage;
            HttpStatus status;
            if (existingUsage.isPresent()) {
                // Upsert behavior: same flock/feed/date/shift updates the existing row.
                usage = existingUsage.get();
                usage.setSacksUsed(request.getSacksUsed());
                usage.setRecordedBy(request.getRecordedBy());
                status = HttpStatus.OK;
            } else {
                usage = new FeedUsage(
                        request.getFlockId(),
                        request.getFeedTypeId(),
                        request.getRecordDate(),
                        request.getSacksUsed(),
                        shift
                );
                usage.setRecordedBy(request.getRecordedBy());
                status = HttpStatus.CREATED;
            }

            FeedUsage saved = feedUsageRepository.save(usage);
            return ResponseEntity.status(status).body(saved);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Could not save feed usage (check duplicates/constraints).",
                    "detail", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
            ));
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Database error while saving feed usage.",
                    "detail", cause != null ? cause.getMessage() : e.getMessage()
            ));
        }
    }

    @GetMapping("/usage")
    public ResponseEntity<List<FeedUsage>> getUsage(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) UUID feedTypeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(feedUsageRepository.findByFlockIdAndUsageDateBetween(flockId, startDate, endDate));
        }
        if (feedTypeId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(feedUsageRepository.findByFeedTypeIdAndUsageDateBetween(feedTypeId, startDate, endDate));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(feedUsageRepository.findByUsageDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(feedUsageRepository.findAll());
    }

    // ===== SALES =====

    @PostMapping("/sales")
    public ResponseEntity<FeedSale> createSale(
            @RequestBody FeedSaleRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireFinancialOrThrow(actorId);
       FeedSale sale = new FeedSale(
    request.getSaleDate(),
    request.getSacksQty(),
    request.getPricePerSack(),
    request.getBuyerName()
);
        sale.setBuyerName(request.getBuyerName());
        sale.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedSaleRepository.save(sale));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<FeedSale>> getSales(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(feedSaleRepository.findBySaleDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(feedSaleRepository.findAll());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<FeedType>> getStockReport() {
        return ResponseEntity.ok(feedTypeRepository.findAll());
    }
}

class FeedPurchaseRequest {
    private UUID feedTypeId;
    private UUID supplierId;
    private LocalDate purchaseDate;
    private Integer sacksQty;
    private java.math.BigDecimal costPerSack;
    private UUID recordedBy;

    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public Integer getSacksQty() { return sacksQty; }
    public void setSacksQty(Integer sacksQty) { this.sacksQty = sacksQty; }
    public java.math.BigDecimal getCostPerSack() { return costPerSack; }
    public void setCostPerSack(java.math.BigDecimal costPerSack) { this.costPerSack = costPerSack; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class FeedUsageRequest {
    private UUID flockId;
    private UUID feedTypeId;
    private LocalDate recordDate;
    private Integer sacksUsed;
    private String shift;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Integer getSacksUsed() { return sacksUsed; }
    public void setSacksUsed(Integer sacksUsed) { this.sacksUsed = sacksUsed; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class FeedSaleRequest {
    private LocalDate saleDate;
    private Integer sacksQty;
    private java.math.BigDecimal pricePerSack;
    private String buyerName;
    private UUID recordedBy;

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public Integer getSacksQty() { return sacksQty; }
    public void setSacksQty(Integer sacksQty) { this.sacksQty = sacksQty; }
    public java.math.BigDecimal getPricePerSack() { return pricePerSack; }
    public void setPricePerSack(java.math.BigDecimal pricePerSack) { this.pricePerSack = pricePerSack; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}