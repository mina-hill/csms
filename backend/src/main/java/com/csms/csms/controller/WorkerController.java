package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
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
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

    @GetMapping
    public ResponseEntity<List<Worker>> getAllWorkers() {
        return ResponseEntity.ok(workerRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Worker>> getActiveWorkers() {
        return ResponseEntity.ok(workerRepository.findByIsActiveTrueOrderByNameAsc());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<Worker>> getWorkersByRole(@PathVariable String role) {
        return ResponseEntity.ok(workerRepository.findByRoleAndIsActive(role, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Worker> getWorkerById(@PathVariable UUID id) {
        Optional<Worker> worker = workerRepository.findById(id);
        return worker.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search/name")
    public ResponseEntity<Worker> getWorkerByName(@RequestParam String name) {
        Optional<Worker> worker = workerRepository.findByName(name);
        return worker.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Worker> createWorker(
            @RequestBody WorkerRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) return ResponseEntity.badRequest().build();
        if (request.getRole() == null || request.getRole().isBlank()) return ResponseEntity.badRequest().build();
        if (request.getJoinDate() == null) return ResponseEntity.badRequest().build();
        if (request.getSalaryRate() == null || request.getSalaryRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Worker worker = new Worker(
                request.getName(),
                request.getRole(),
                request.getContact(),
                request.getJoinDate(),
                request.getSalaryRate()
        );
        if (request.getIsActive() != null) worker.setIsActive(request.getIsActive());
        return ResponseEntity.status(HttpStatus.CREATED).body(workerRepository.save(worker));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Worker> updateWorker(
            @PathVariable UUID id,
            @RequestBody WorkerRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        Optional<Worker> existingWorker = workerRepository.findById(id);
        if (existingWorker.isEmpty()) return ResponseEntity.notFound().build();

        Worker worker = existingWorker.get();
        if (request.getName() != null && !request.getName().isBlank()) worker.setName(request.getName());
        if (request.getRole() != null && !request.getRole().isBlank()) worker.setRole(request.getRole());
        if (request.getContact() != null) worker.setContact(request.getContact());
        if (request.getJoinDate() != null) worker.setJoinDate(request.getJoinDate());
        if (request.getSalaryRate() != null) {
            if (request.getSalaryRate().compareTo(BigDecimal.ZERO) <= 0) return ResponseEntity.badRequest().build();
            worker.setSalaryRate(request.getSalaryRate());
        }
        if (request.getIsActive() != null) worker.setIsActive(request.getIsActive());

        return ResponseEntity.ok(workerRepository.save(worker));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(
            @PathVariable UUID id,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        Optional<Worker> worker = workerRepository.findById(id);
        if (worker.isEmpty()) return ResponseEntity.notFound().build();

        Worker w = worker.get();
        w.setIsActive(false);
        workerRepository.save(w);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveWorkerCount() {
        return ResponseEntity.ok(workerRepository.countByIsActive(true));
    }
}

class WorkerRequest {
    private String name;
    private String role;
    private String contact;
    private LocalDate joinDate;
    private BigDecimal salaryRate;
    private Boolean isActive;

    public WorkerRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public BigDecimal getSalaryRate() { return salaryRate; }
    public void setSalaryRate(BigDecimal salaryRate) { this.salaryRate = salaryRate; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
