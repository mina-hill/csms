package com.csms.csms.controller;

import com.csms.csms.entity.Flock;
import com.csms.csms.entity.FlockSale;
import com.csms.csms.entity.FlockStatus;
import com.csms.csms.entity.OtherSale;
import com.csms.csms.entity.OtherSaleCategory;
import com.csms.csms.repository.FlockRepository;
import com.csms.csms.repository.FlockSaleRepository;
import com.csms.csms.repository.OtherSaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private FlockSaleRepository flockSaleRepository;

    @Autowired
    private OtherSaleRepository otherSaleRepository;

    // FIX-6: Need FlockRepository to check flock status before saving a sale.
    @Autowired
    private FlockRepository flockRepository;

    // ===== FLOCK SALES =====

    /**
     * POST /api/sales/flock
     * FIX-6: Returns 409 if flock is CLOSED.
     * FIX-7: Returns 400 if required fields are missing or invalid.
     */
    @PostMapping("/flock")
    public ResponseEntity<?> createFlockSale(@RequestBody FlockSaleRequest request) {
        // FIX-7: Input validation
        if (request.getFlockId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "flockId is required"));
        }
        if (request.getSaleDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "saleDate is required"));
        }
        if (request.getBuyerName() == null || request.getBuyerName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "buyerName is required"));
        }
        if (request.getQtySold() == null || request.getQtySold() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "qtySold must be a positive integer"));
        }
        if (request.getWeightPerBirdKg() == null || request.getWeightPerBirdKg().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "weightPerBirdKg must be positive"));
        }
        if (request.getPricePerKg() == null || request.getPricePerKg().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "pricePerKg must be positive"));
        }

        // FIX-6: Reject sale if flock is CLOSED — return 409 Conflict
        Optional<Flock> flockOpt = flockRepository.findById(request.getFlockId());
        if (flockOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Flock not found: " + request.getFlockId()));
        }
        if (flockOpt.get().getStatus() == FlockStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Cannot record a sale for a CLOSED flock"));
        }

        FlockSale sale = new FlockSale(
                request.getFlockId(),
                request.getSaleDate(),
                request.getBuyerName(),
                request.getQtySold(),
                request.getWeightPerBirdKg(),
                request.getPricePerKg()
        );
        sale.setRecordedBy(request.getRecordedBy());

        return ResponseEntity.status(HttpStatus.CREATED).body(flockSaleRepository.save(sale));
    }

    @GetMapping("/flock")
    public ResponseEntity<List<FlockSale>> getFlockSales(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(flockSaleRepository.findByFlockIdAndSaleDateBetween(flockId, startDate, endDate));
        }
        if (flockId != null) {
            return ResponseEntity.ok(flockSaleRepository.findByFlockId(flockId));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(flockSaleRepository.findBySaleDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(flockSaleRepository.findAll());
    }

    @GetMapping("/flock/{id}")
    public ResponseEntity<FlockSale> getFlockSaleById(@PathVariable UUID id) {
        return flockSaleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== OTHER SALES =====

    /**
     * POST /api/sales/other
     * FIX-8: OtherSaleCategory is now a proper import, not inline qualified name.
     * FIX-9: description is NOT NULL in SQL — validated before save.
     */
    @PostMapping("/other")
    public ResponseEntity<?> createOtherSale(@RequestBody SalesOtherSaleRequest request) {
        // FIX-9: description is NOT NULL in the other_sales table
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "description is required"));
        }
        if (request.getCategory() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "category is required"));
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount must be positive"));
        }
        if (request.getSaleDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "saleDate is required"));
        }

        String buyer = request.getBuyerName() != null ? request.getBuyerName().trim() : null;
        OtherSale sale = new OtherSale(
                request.getSaleDate(),
                request.getCategory(),
                request.getDescription().trim(),
                buyer,
                request.getAmount()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(otherSaleRepository.save(sale));
    }

    @GetMapping("/other")
    public ResponseEntity<List<OtherSale>> getOtherSales(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(otherSaleRepository.findBySaleDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(otherSaleRepository.findAll());
    }

    @GetMapping("/other/{id}")
    public ResponseEntity<OtherSale> getOtherSaleById(@PathVariable UUID id) {
        return otherSaleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== SALES SUMMARY =====

    @GetMapping("/summary")
    public ResponseEntity<SalesSummary> getSalesSummary(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<FlockSale> flockSales;
        List<OtherSale> otherSales;

        if (startDate != null && endDate != null) {
            flockSales = flockSaleRepository.findBySaleDateBetween(startDate, endDate);
            otherSales = otherSaleRepository.findBySaleDateBetween(startDate, endDate);
        } else {
            flockSales = flockSaleRepository.findAll();
            otherSales = otherSaleRepository.findAll();
        }

        BigDecimal flockRevenue = flockSales.stream()
                .map(FlockSale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otherRevenue = otherSales.stream()
                .map(OtherSale::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(new SalesSummary(
                flockSales.size(), flockRevenue,
                otherSales.size(), otherRevenue,
                flockRevenue.add(otherRevenue)
        ));
    }
}

// ===== DTOs =====

class FlockSaleRequest {
    private UUID flockId;
    private LocalDate saleDate;
    private String buyerName;
    private Integer qtySold;
    private BigDecimal weightPerBirdKg;
    private BigDecimal pricePerKg;
    private UUID recordedBy;

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public Integer getQtySold() { return qtySold; }
    public void setQtySold(Integer qtySold) { this.qtySold = qtySold; }
    public BigDecimal getWeightPerBirdKg() { return weightPerBirdKg; }
    public void setWeightPerBirdKg(BigDecimal weightPerBirdKg) { this.weightPerBirdKg = weightPerBirdKg; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

// FIX-8: OtherSaleCategory is now a proper import at the top of the file — no inline qualified name.
class SalesOtherSaleRequest {
    private OtherSaleCategory category;
    private BigDecimal amount;
    private LocalDate saleDate;
    private String description;
    private String buyerName;

    public OtherSaleCategory getCategory() { return category; }
    public void setCategory(OtherSaleCategory category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
}

class SalesSummary {
    private int flockSalesCount;
    private BigDecimal flockRevenue;
    private int otherSalesCount;
    private BigDecimal otherRevenue;
    private BigDecimal totalRevenue;

    public SalesSummary(int flockSalesCount, BigDecimal flockRevenue,
                        int otherSalesCount, BigDecimal otherRevenue,
                        BigDecimal totalRevenue) {
        this.flockSalesCount = flockSalesCount;
        this.flockRevenue = flockRevenue;
        this.otherSalesCount = otherSalesCount;
        this.otherRevenue = otherRevenue;
        this.totalRevenue = totalRevenue;
    }

    public int getFlockSalesCount() { return flockSalesCount; }
    public void setFlockSalesCount(int flockSalesCount) { this.flockSalesCount = flockSalesCount; }
    public BigDecimal getFlockRevenue() { return flockRevenue; }
    public void setFlockRevenue(BigDecimal flockRevenue) { this.flockRevenue = flockRevenue; }
    public int getOtherSalesCount() { return otherSalesCount; }
    public void setOtherSalesCount(int otherSalesCount) { this.otherSalesCount = otherSalesCount; }
    public BigDecimal getOtherRevenue() { return otherRevenue; }
    public void setOtherRevenue(BigDecimal otherRevenue) { this.otherRevenue = otherRevenue; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}
