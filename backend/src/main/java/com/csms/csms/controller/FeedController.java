package com.csms.csms.controller;

import com.csms.csms.entity.FeedPurchase;
import com.csms.csms.entity.FeedUsage;
import com.csms.csms.entity.FeedSale;
import com.csms.csms.entity.FeedType;
import com.csms.csms.repository.FeedPurchaseRepository;
import com.csms.csms.repository.FeedUsageRepository;
import com.csms.csms.repository.FeedSaleRepository;
import com.csms.csms.repository.FeedTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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
    public ResponseEntity<FeedPurchase> createPurchase(@RequestBody FeedPurchaseRequest request) {
        FeedPurchase purchase = new FeedPurchase(
            request.getFeedTypeId(),
            request.getSupplierId(),
            request.getPurchaseDate(),
            request.getSacksQty(),
            request.getCostPerSack()
        );
        purchase.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedPurchaseRepository.save(purchase));
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
    public ResponseEntity<FeedUsage> createUsage(@RequestBody FeedUsageRequest request) {
        FeedUsage usage = new FeedUsage(
            request.getFlockId(),
            request.getFeedTypeId(),
            request.getRecordDate(),
            request.getSacksUsed()
        );
        usage.setRecordedBy(request.getRecordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedUsageRepository.save(usage));
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
    public ResponseEntity<FeedSale> createSale(@RequestBody FeedSaleRequest request) {
       FeedSale sale = new FeedSale(
    request.getFeedTypeId(),
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
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Integer getSacksUsed() { return sacksUsed; }
    public void setSacksUsed(Integer sacksUsed) { this.sacksUsed = sacksUsed; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

class FeedSaleRequest {
    private UUID feedTypeId;
    private LocalDate saleDate;
    private Integer sacksQty;
    private java.math.BigDecimal pricePerSack;
    private String buyerName;
    private UUID recordedBy;

    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }
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