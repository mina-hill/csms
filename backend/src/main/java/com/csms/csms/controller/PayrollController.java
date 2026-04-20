package com.csms.csms.controller;

import com.csms.csms.entity.SalaryPayment;
import com.csms.csms.entity.Worker;
import com.csms.csms.repository.SalaryPaymentRepository;
import com.csms.csms.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "*")
public class PayrollController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestBody PayrollProcessRequest request) {
        if (request.getWorkerId() == null || request.getEndDate() == null) {
            return ResponseEntity.badRequest().body("workerId and endDate are required.");
        }
        UUID workerUuid;
        try {
            workerUuid = UUID.fromString(request.getWorkerId().trim());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("workerId must be a valid UUID.");
        }

        Worker worker = workerRepository.findById(workerUuid).orElse(null);
        if (worker == null || Boolean.FALSE.equals(worker.getIsActive())) {
            return ResponseEntity.badRequest().body("Selected worker is missing or inactive.");
        }
        LocalDate derivedStartDate = worker.getJoinDate();
        if (derivedStartDate == null) {
            return ResponseEntity.badRequest().body("Selected worker has no joinDate.");
        }
        if (request.getEndDate().isBefore(derivedStartDate)) {
            return ResponseEntity.badRequest().body("endDate must be on or after worker joinDate.");
        }
        int computedDaysWorked = (int) ChronoUnit.DAYS.between(derivedStartDate, request.getEndDate()) + 1;
        if (computedDaysWorked <= 0) {
            return ResponseEntity.badRequest().body("Computed daysWorked must be greater than 0.");
        }

        int periodYear = request.getEndDate().getYear();
        int periodMonth = request.getEndDate().getMonthValue();
        List<PayrollWorkerLineItem> workerItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        BigDecimal monthlySalary = worker.getSalaryRate();
        BigDecimal dailyRate = monthlySalary.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(computedDaysWorked)).setScale(2, RoundingMode.HALF_UP);

        SalaryPayment payment = salaryPaymentRepository
                .findByWorkerIdAndPeriodYearAndPeriodMonth(worker.getWorkerId(), periodYear, periodMonth)
                .orElseGet(SalaryPayment::new);

        payment.setWorker(worker);
        payment.setPeriodYear(periodYear);
        payment.setPeriodMonth(periodMonth);
        payment.setPeriodStartDate(derivedStartDate);
        payment.setPeriodEndDate(request.getEndDate());
        payment.setDaysWorked(computedDaysWorked);
        payment.setMonthlySalary(monthlySalary);
        payment.setDailyRate(dailyRate);
        payment.setAmountPaid(amount);
        payment.setStatus("PROCESSED");
        payment.setProcessedAt(OffsetDateTime.now());

        SalaryPayment savedPayment = salaryPaymentRepository.save(payment);
        totalAmount = totalAmount.add(amount);

        workerItems.add(new PayrollWorkerLineItem(
                savedPayment.getId(),
                worker.getWorkerId(),
                worker.getName(),
                worker.getRole(),
                monthlySalary,
                dailyRate,
                computedDaysWorked,
                amount
        ));

        PayrollReportItem report = new PayrollReportItem(
                periodYear,
                periodMonth,
                derivedStartDate,
                request.getEndDate(),
                computedDaysWorked,
                workerItems,
                totalAmount,
                OffsetDateTime.now(),
                "PROCESSED"
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/report")
    public ResponseEntity<List<PayrollReportItem>> getPayrollReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        List<SalaryPayment> payments;
        if (year != null && month != null) {
            payments = salaryPaymentRepository.findByPeriodYearAndPeriodMonth(year, month);
        } else if (year != null) {
            payments = salaryPaymentRepository.findByPeriodYear(year);
        } else {
            payments = salaryPaymentRepository.findAllByOrderByPeriodYearDescPeriodMonthDescProcessedAtDesc();
        }

        Map<String, List<SalaryPayment>> grouped = new LinkedHashMap<>();
        for (SalaryPayment payment : payments) {
            String key = payment.getPeriodYear() + "-" + payment.getPeriodMonth() + "-" + payment.getDaysWorked();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(payment);
        }

        List<PayrollReportItem> reportItems = new ArrayList<>();
        for (List<SalaryPayment> periodPayments : grouped.values()) {
            if (periodPayments.isEmpty()) continue;

            SalaryPayment first = periodPayments.get(0);
            BigDecimal total = BigDecimal.ZERO;
            List<PayrollWorkerLineItem> workerLines = new ArrayList<>();

            for (SalaryPayment payment : periodPayments) {
                Worker worker = payment.getWorker();
                workerLines.add(new PayrollWorkerLineItem(
                        payment.getId(),
                        worker != null ? worker.getWorkerId() : null,
                        worker != null ? worker.getName() : "Unknown Worker",
                        worker != null ? worker.getRole() : "Unknown",
                        payment.getMonthlySalary(),
                        payment.getDailyRate(),
                        payment.getDaysWorked(),
                        payment.getAmountPaid()
                ));
                total = total.add(payment.getAmountPaid() == null ? BigDecimal.ZERO : payment.getAmountPaid());
            }

            reportItems.add(new PayrollReportItem(
                    first.getPeriodYear(),
                    first.getPeriodMonth(),
                    first.getPeriodStartDate(),
                    first.getPeriodEndDate(),
                    first.getDaysWorked(),
                    workerLines,
                    total,
                    first.getProcessedAt(),
                    first.getStatus()
            ));
        }

        return ResponseEntity.ok(reportItems);
    }
}

class PayrollProcessRequest {
    private LocalDate endDate;
    private String workerId;

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}

class PayrollReportItem {
    private Integer periodYear;
    private Integer periodMonth;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysWorked;
    private List<PayrollWorkerLineItem> workers;
    private BigDecimal totalAmount;
    private OffsetDateTime processedAt;
    private String status;

    public PayrollReportItem(Integer periodYear, Integer periodMonth, LocalDate startDate, LocalDate endDate,
                             Integer daysWorked, List<PayrollWorkerLineItem> workers, BigDecimal totalAmount,
                             OffsetDateTime processedAt, String status) {
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysWorked = daysWorked;
        this.workers = workers;
        this.totalAmount = totalAmount;
        this.processedAt = processedAt;
        this.status = status;
    }

    public Integer getPeriodYear() { return periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getDaysWorked() { return daysWorked; }
    public List<PayrollWorkerLineItem> getWorkers() { return workers; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public String getStatus() { return status; }
}

class PayrollWorkerLineItem {
    private UUID paymentId;
    private UUID workerId;
    private String workerName;
    private String role;
    private BigDecimal monthlySalary;
    private BigDecimal dailyRate;
    private Integer daysWorked;
    private BigDecimal amount;

    public PayrollWorkerLineItem(UUID paymentId, UUID workerId, String workerName, String role,
                                 BigDecimal monthlySalary, BigDecimal dailyRate, Integer daysWorked, BigDecimal amount) {
        this.paymentId = paymentId;
        this.workerId = workerId;
        this.workerName = workerName;
        this.role = role;
        this.monthlySalary = monthlySalary;
        this.dailyRate = dailyRate;
        this.daysWorked = daysWorked;
        this.amount = amount;
    }

    public UUID getPaymentId() { return paymentId; }
    public UUID getWorkerId() { return workerId; }
    public String getWorkerName() { return workerName; }
    public String getRole() { return role; }
    public BigDecimal getMonthlySalary() { return monthlySalary; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public Integer getDaysWorked() { return daysWorked; }
    public BigDecimal getAmount() { return amount; }
}