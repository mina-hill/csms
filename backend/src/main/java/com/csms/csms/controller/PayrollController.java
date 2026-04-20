package com.csms.csms.controller;

import com.csms.csms.entity.Worker;
import com.csms.csms.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/workers")
@CrossOrigin(origins = "*")
public class PayrollController {

    @Autowired
    private WorkerRepository workerRepository;

    /**
     * GET /api/workers - Get all workers
     * Called by: Payroll dashboard to display worker list
     */
    @GetMapping
    public ResponseEntity<List<Worker>> getAllWorkers() {
        List<Worker> workers = workerRepository.findAll();
        return ResponseEntity.ok(workers);
    }

    /**
     * GET /api/workers/active - Get all active workers
     * Called by: Payroll calculation (only active workers get paid)
     */
    @GetMapping("/active")
    public ResponseEntity<List<Worker>> getActiveWorkers() {
        List<Worker> workers = workerRepository.findByIsActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(workers);
    }

    /**
     * GET /api/workers/role/{role} - Get workers by role
     * Example: GET /api/workers/role/Shed%20Manager
     * Called by: Filter workers by job title
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<Worker>> getWorkersByRole(@PathVariable String role) {
        List<Worker> workers = workerRepository.findByRoleAndIsActive(role, true);
        return ResponseEntity.ok(workers);
    }

    /**
     * GET /api/workers/{id} - Get single worker by ID
     * Called by: View worker details, edit form
     */
    @GetMapping("/{id}")
    public ResponseEntity<Worker> getWorkerById(@PathVariable UUID id) {
        Optional<Worker> worker = workerRepository.findById(id);
        if (worker.isPresent()) {
            return ResponseEntity.ok(worker.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/workers/search/name?name=John%20Doe
     * Called by: Search worker by name
     */
    @GetMapping("/search/name")
    public ResponseEntity<Worker> getWorkerByName(@RequestParam String name) {
        Optional<Worker> worker = workerRepository.findByName(name);
        if (worker.isPresent()) {
            return ResponseEntity.ok(worker.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * POST /api/workers - Create new worker
     * US-023: Add new worker to payroll system
     * Request body: { "name": "John Doe", "role": "Shed Manager", "contact": "555-1234", "joinDate": "2024-01-01", "salaryRate": 15000.00 }
     */
    @PostMapping
    public ResponseEntity<Worker> createWorker(@RequestBody WorkerRequest request) {
        // Validate required fields
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getRole() == null || request.getRole().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getJoinDate() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getSalaryRate() == null || request.getSalaryRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(null); // Salary must be > 0
        }

        // Warn if duplicate name+role exists (but allow creation)
        if (workerRepository.existsByNameAndRole(request.getName(), request.getRole())) {
            // Log warning but proceed
            System.out.println("Warning: Worker with name " + request.getName() + " and role " + request.getRole() + " already exists");
        }

        Worker worker = new Worker(
            request.getName(),
            request.getRole(),
            request.getContact(),
            request.getJoinDate(),
            request.getSalaryRate()
        );

        if (request.getIsActive() != null) {
            worker.setIsActive(request.getIsActive());
        }

        Worker savedWorker = workerRepository.save(worker);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWorker);
    }

    /**
     * PUT /api/workers/{id} - Update worker
     * US-023: Update worker profile and salary rate
     * Request body: { "name": "John Doe", "role": "Senior Shed Manager", "contact": "555-9999", "joinDate": "2024-01-01", "salaryRate": 18000.00, "isActive": true }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Worker> updateWorker(@PathVariable UUID id, @RequestBody WorkerRequest request) {
        Optional<Worker> existingWorker = workerRepository.findById(id);

        if (!existingWorker.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Worker worker = existingWorker.get();

        // Update name if provided
        if (request.getName() != null && !request.getName().isBlank()) {
            worker.setName(request.getName());
        }

        // Update role if provided
        if (request.getRole() != null && !request.getRole().isBlank()) {
            worker.setRole(request.getRole());
        }

        // Update contact if provided
        if (request.getContact() != null) {
            worker.setContact(request.getContact());
        }

        // Update join date if provided
        if (request.getJoinDate() != null) {
            worker.setJoinDate(request.getJoinDate());
        }

        // Update salary rate if provided (validate > 0)
        if (request.getSalaryRate() != null) {
            if (request.getSalaryRate().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().build(); // Salary must be > 0
            }
            worker.setSalaryRate(request.getSalaryRate());
        }

        // Update active status if provided
        if (request.getIsActive() != null) {
            worker.setIsActive(request.getIsActive());
        }

        Worker updatedWorker = workerRepository.save(worker);
        return ResponseEntity.ok(updatedWorker);
    }

    /**
     * DELETE /api/workers/{id} - Soft delete worker (mark inactive)
     * Note: We mark as inactive instead of hard delete to preserve payroll history
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(@PathVariable UUID id) {
        Optional<Worker> worker = workerRepository.findById(id);

        if (!worker.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Worker w = worker.get();
        w.setIsActive(false);
        workerRepository.save(w);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/workers/count/active - Get count of active workers
     * Called by: Dashboard statistics
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveWorkerCount() {
        long count = workerRepository.countByIsActive(true);
        return ResponseEntity.ok(count);
    }
}

/**
 * Request DTO - What frontend sends to backend
 */
class WorkerRequest {
    private String name;
    private String role;
    private String contact;
    private LocalDate joinDate;
    private BigDecimal salaryRate;
    private Boolean isActive;

    // Constructors
    public WorkerRequest() {}

    public WorkerRequest(String name, String role, LocalDate joinDate, BigDecimal salaryRate) {
        this.name = name;
        this.role = role;
        this.joinDate = joinDate;
        this.salaryRate = salaryRate;
        this.isActive = true;
    }

    // Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public BigDecimal getSalaryRate() {
        return salaryRate;
    }

    public void setSalaryRate(BigDecimal salaryRate) {
        this.salaryRate = salaryRate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}