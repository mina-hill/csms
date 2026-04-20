package com.csms.csms.controller;

import com.csms.csms.entity.Supplier;
import com.csms.csms.entity.SupplierType;
import com.csms.csms.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepository;

    /**
     * GET /api/suppliers - Get all suppliers
     * Called by: UI to display supplier list
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * GET /api/suppliers/active - Get all active suppliers
     * Called by: Forms needing supplier dropdown
     */
    @GetMapping("/active")
    public ResponseEntity<List<Supplier>> getActiveSuppliers() {
        List<Supplier> suppliers = supplierRepository.findByIsActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * GET /api/suppliers/type/{type} - Get suppliers by type
     * Example: GET /api/suppliers/type/FEED
     * Called by: Filtering suppliers by category
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Supplier>> getSuppliersByType(@PathVariable SupplierType type) {
        List<Supplier> suppliers = supplierRepository.findBySupplierTypeAndIsActive(type, true);
        return ResponseEntity.ok(suppliers);
    }

    /**
     * GET /api/suppliers/{id} - Get single supplier by ID
     * Called by: View supplier details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable UUID id) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        if (supplier.isPresent()) {
            return ResponseEntity.ok(supplier.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/suppliers/search/name?name=Acme%20Feeds
     * Called by: Search supplier by name
     */
    @GetMapping("/search/name")
    public ResponseEntity<Supplier> getSupplierByName(@RequestParam String name) {
        Optional<Supplier> supplier = supplierRepository.findByName(name);
        if (supplier.isPresent()) {
            return ResponseEntity.ok(supplier.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * POST /api/suppliers - Create new supplier
     * US-007: Add new supplier
     * Request body: { "name": "Acme Feeds", "phone": "123-456", "address": "123 Farm Rd", "supplierType": "FEED" }
     */
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody SupplierRequest request) {
        // Validate required fields
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Check if supplier name already exists (duplicate validation)
        if (supplierRepository.existsByName(request.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }

        Supplier supplier = new Supplier(
            request.getName(),
            request.getPhone(),
            request.getAddress(),
            request.getSupplierType()
        );

        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }

        Supplier savedSupplier = supplierRepository.save(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSupplier);
    }

    /**
     * PUT /api/suppliers/{id} - Update supplier
     * US-007: Update supplier information
     * Request body: { "name": "Updated Name", "phone": "999-888", "address": "New Address", "supplierType": "MEDICINE", "isActive": true }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable UUID id, @RequestBody SupplierRequest request) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        
        if (!existingSupplier.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Supplier supplier = existingSupplier.get();

        // Check if new name conflicts with another supplier (if name is being changed)
        if (request.getName() != null && !request.getName().equals(supplier.getName())) {
            if (supplierRepository.existsByName(request.getName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
            }
            supplier.setName(request.getName());
        }

        // Update other fields if provided
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        if (request.getSupplierType() != null) {
            supplier.setSupplierType(request.getSupplierType());
        }
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);
        return ResponseEntity.ok(updatedSupplier);
    }

    /**
     * DELETE /api/suppliers/{id} - Soft delete supplier (mark inactive)
     * Note: We mark as inactive instead of hard delete to preserve historical data
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable UUID id) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        
        if (!supplier.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Supplier s = supplier.get();
        s.setIsActive(false);
        supplierRepository.save(s);
        
        return ResponseEntity.noContent().build();
    }
}

/**
 * Request DTO - What frontend sends to backend
 */
class SupplierRequest {
    private String name;
    private String phone;
    private String address;
    private SupplierType supplierType;
    private Boolean isActive;

    // Constructors
    public SupplierRequest() {}

    public SupplierRequest(String name, SupplierType supplierType) {
        this.name = name;
        this.supplierType = supplierType;
        this.isActive = true;
    }

    // Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SupplierType getSupplierType() {
        return supplierType;
    }

    public void setSupplierType(SupplierType supplierType) {
        this.supplierType = supplierType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}