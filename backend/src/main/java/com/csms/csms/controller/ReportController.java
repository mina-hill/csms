package com.csms.csms.controller;

import com.csms.csms.entity.*;
import com.csms.csms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private VFcrReportRepository vFcrReportRepository;
    
    @Autowired
    private VMortalityReportRepository vMortalityReportRepository;
    
    @Autowired
    private VProfitLossRepository vProfitLossRepository;
    
    @Autowired
    private VGlobalProfitLossRepository vGlobalProfitLossRepository;
    
    @Autowired
    private VResourceConsumptionRepository vResourceConsumptionRepository;

    // ===== FCR REPORT (US-029) =====

    @GetMapping("/fcr")
    public ResponseEntity<List<VFcrReport>> getFcrReport() {
        return ResponseEntity.ok(vFcrReportRepository.findAll());
    }

    @GetMapping("/fcr/{flockId}")
    public ResponseEntity<VFcrReport> getFcrByFlock(@PathVariable UUID flockId) {
        return vFcrReportRepository.findById(flockId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== MORTALITY REPORT (US-028) =====

    @GetMapping("/mortality")
    public ResponseEntity<List<VMortalityReport>> getMortalityReport(
            @RequestParam(required = false) UUID flockId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (flockId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(vMortalityReportRepository.findByFlockIdAndRecordDateBetween(flockId, startDate, endDate));
        }
        if (flockId != null) {
            return ResponseEntity.ok(vMortalityReportRepository.findByFlockId(flockId));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(vMortalityReportRepository.findByRecordDateBetween(startDate, endDate));
        }
        return ResponseEntity.ok(vMortalityReportRepository.findAll());
    }

    // ===== PROFIT & LOSS REPORT (US-030) =====

    @GetMapping("/profit-loss")
    public ResponseEntity<List<VProfitLoss>> getProfitLossReport() {
        return ResponseEntity.ok(vProfitLossRepository.findAll());
    }

    @GetMapping("/profit-loss/{flockId}")
    public ResponseEntity<VProfitLoss> getProfitLossByFlock(@PathVariable UUID flockId) {
        return vProfitLossRepository.findById(flockId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== GLOBAL PROFIT & LOSS =====

    @GetMapping("/profit-loss/global/summary")
    public ResponseEntity<List<VGlobalProfitLoss>> getGlobalProfitLoss() {
        return ResponseEntity.ok(vGlobalProfitLossRepository.findAll());
    }

    // ===== RESOURCE CONSUMPTION (US-032) =====

    @GetMapping("/resource-consumption")
    public ResponseEntity<List<VResourceConsumption>> getResourceConsumption() {
        return ResponseEntity.ok(vResourceConsumptionRepository.findAll());
    }

    @GetMapping("/resource-consumption/{flockId}")
    public ResponseEntity<List<VResourceConsumption>> getResourceConsumptionByFlock(@PathVariable UUID flockId) {
        return ResponseEntity.ok(vResourceConsumptionRepository.findByFlockId(flockId));
    }

    @GetMapping("/resource-consumption/type/{resourceType}")
    public ResponseEntity<List<VResourceConsumption>> getResourceByType(@PathVariable String resourceType) {
        return ResponseEntity.ok(vResourceConsumptionRepository.findByResourceType(resourceType));
    }
}