package com.csms.csms.controller;

import com.csms.csms.entity.OtherSale;
import com.csms.csms.entity.OtherSaleCategory;
import com.csms.csms.repository.OtherSaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/other-sales")
@CrossOrigin(origins = "*")
public class OtherSaleController {

    @Autowired
    private OtherSaleRepository otherSaleRepository;

    @GetMapping
    public ResponseEntity<List<OtherSale>> getOtherSales(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(otherSaleRepository.findBySaleDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(otherSaleRepository.findAllByOrderBySaleDateDescSaleIdDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OtherSale> getOtherSaleById(@PathVariable java.util.UUID id) {
        return otherSaleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOtherSale(@RequestBody OtherSaleRequest request) {
        if (request.getSaleDate() == null) return ResponseEntity.badRequest().body("saleDate is required.");
        OtherSaleCategory normalizedCategory = request.getCategoryEnum();
        if (normalizedCategory == null) return ResponseEntity.badRequest().body("category is required.");
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body("description is required.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("amount must be greater than 0.");
        }

        OtherSale sale = new OtherSale(
                request.getSaleDate(),
                normalizedCategory,
                request.getDescription().trim(),
                request.getBuyer() == null ? null : request.getBuyer().trim(),
                request.getAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(otherSaleRepository.save(sale));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOtherSale(@PathVariable java.util.UUID id, @RequestBody OtherSaleRequest request) {
        return otherSaleRepository.findById(id).map(existing -> {
            if (request.getSaleDate() != null) existing.setSaleDate(request.getSaleDate());
            if (request.getCategoryEnum() != null) existing.setCategory(request.getCategoryEnum());
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                existing.setDescription(request.getDescription().trim());
            }
            if (request.getBuyer() != null) existing.setBuyer(request.getBuyer().trim());
            if (request.getAmount() != null) {
                if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body("amount must be greater than 0.");
                }
                existing.setAmount(request.getAmount());
            }
            return ResponseEntity.ok(otherSaleRepository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/report")
    public ResponseEntity<OtherSaleReport> getOtherSaleReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        List<OtherSale> sales;
        if (startDate != null && endDate != null) {
            sales = otherSaleRepository.findBySaleDateBetween(startDate, endDate);
        } else {
            sales = otherSaleRepository.findAllByOrderBySaleDateDescSaleIdDesc();
        }

        BigDecimal total = BigDecimal.ZERO;
        Map<OtherSaleCategory, BigDecimal> byCategory = new EnumMap<>(OtherSaleCategory.class);
        for (OtherSaleCategory category : OtherSaleCategory.values()) {
            byCategory.put(category, BigDecimal.ZERO);
        }

        for (OtherSale sale : sales) {
            BigDecimal amount = sale.getAmount() == null ? BigDecimal.ZERO : sale.getAmount();
            total = total.add(amount);
            byCategory.put(sale.getCategory(), byCategory.get(sale.getCategory()).add(amount));
        }

        List<OtherSaleCategoryTotal> categoryTotals = new ArrayList<>();
        for (Map.Entry<OtherSaleCategory, BigDecimal> entry : byCategory.entrySet()) {
            categoryTotals.add(new OtherSaleCategoryTotal(entry.getKey(), entry.getValue()));
        }

        return ResponseEntity.ok(new OtherSaleReport(sales, total, categoryTotals));
    }
}

class OtherSaleRequest {
    private LocalDate saleDate;
    private String category;
    private String description;
    private String buyer;
    private BigDecimal amount;

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = buyer; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OtherSaleCategory getCategoryEnum() {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("BYPRODUCT".equals(normalized)) normalized = "BY_PRODUCT";
        try {
            return OtherSaleCategory.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

class OtherSaleReport {
    private List<OtherSale> sales;
    private BigDecimal totalAmount;
    private List<OtherSaleCategoryTotal> categoryTotals;

    public OtherSaleReport(List<OtherSale> sales, BigDecimal totalAmount, List<OtherSaleCategoryTotal> categoryTotals) {
        this.sales = sales;
        this.totalAmount = totalAmount;
        this.categoryTotals = categoryTotals;
    }

    public List<OtherSale> getSales() { return sales; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OtherSaleCategoryTotal> getCategoryTotals() { return categoryTotals; }
}

class OtherSaleCategoryTotal {
    private OtherSaleCategory category;
    private BigDecimal totalAmount;

    public OtherSaleCategoryTotal(OtherSaleCategory category, BigDecimal totalAmount) {
        this.category = category;
        this.totalAmount = totalAmount;
    }

    public OtherSaleCategory getCategory() { return category; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
