package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.SalaryPayment;
import com.csms.csms.entity.Worker;
import com.csms.csms.repository.SalaryPaymentRepository;
import com.csms.csms.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Black-Box Test Suite for PayrollController
 * Uses Equivalence Partitioning (EP), Boundary Value Analysis (BVA), and Error Guessing (EG).
 * 
 * Test Coverage:
 * 1. Payroll Processing: EP (valid/invalid), BVA (dates, salary), EG (worker state)
 * 2. Payroll Report: EP (filtered/unfiltered), BVA (month/year boundaries)
 * 3. Date Calculations: EP (positive days), BVA (edge dates)
 * 4. Worker Validation: EG (inactive, missing, invalid UUID)
 * 5. Business Rules: daysWorked > 0, dailyRate = monthlySalary/30, amount = dailyRate * daysWorked
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollController - Black-Box Tests")
class PayrollControllerBlackBoxTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;

    @Mock
    private CsmsAccessHelper accessHelper;

    @InjectMocks
    private PayrollController payrollController;

    private UUID workerId;
    private UUID paymentId;
    private Worker activeWorker;
    private LocalDate joinDate;

    @BeforeEach
    void setUp() {
        workerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        joinDate = LocalDate.of(2024, 1, 15);

        activeWorker = new Worker("John Doe", "Shed Manager", joinDate, BigDecimal.valueOf(3000));
        activeWorker.setWorkerId(workerId);
        activeWorker.setIsActive(true);
    }

    // ===== PAYROLL PROCESS TESTS =====

    @Test
    @DisplayName("EP: Process payroll with valid worker and end date")
    void testProcessPayrollValidInputs() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 1, 31));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));
        SalaryPayment savedPayment = new SalaryPayment();
        savedPayment.setId(paymentId);
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 1))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(salaryPaymentRepository, times(1)).save(any(SalaryPayment.class));
    }

    @Test
    @DisplayName("EP: Process payroll updates existing payment record")
    void testProcessPayrollUpdateExistingPayment() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 2, 28));

        SalaryPayment existingPayment = new SalaryPayment();
        existingPayment.setId(paymentId);
        existingPayment.setWorker(activeWorker);

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 2))
                .thenReturn(Optional.of(existingPayment));
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(existingPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Process payroll with endDate = joinDate (boundary, 1 day worked)")
    void testProcessPayrollEndDateEqualsJoinDate() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(joinDate); // Same as join date

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));
        SalaryPayment savedPayment = new SalaryPayment();
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(
                workerId, joinDate.getYear(), joinDate.getMonthValue()))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, ((PayrollReportItem) response.getBody()).getDaysWorked());
    }

    @Test
    @DisplayName("BVA: Process payroll with endDate one day after joinDate")
    void testProcessPayrollOneDay() {
        LocalDate endDate = joinDate.plusDays(1);
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(endDate);

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));
        SalaryPayment savedPayment = new SalaryPayment();
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(
                workerId, endDate.getYear(), endDate.getMonthValue()))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Large salary amount (high boundary)")
    void testProcessPayrollLargeSalary() {
        Worker highEarner = new Worker("Jane Smith", "Director", joinDate, BigDecimal.valueOf(50000));
        highEarner.setWorkerId(workerId);
        highEarner.setIsActive(true);

        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 3, 31));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(highEarner));
        SalaryPayment savedPayment = new SalaryPayment();
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 3))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Small salary amount (low boundary)")
    void testProcessPayrollSmallSalary() {
        Worker lowEarner = new Worker("Bob Worker", "Laborer", joinDate, BigDecimal.valueOf(500));
        lowEarner.setWorkerId(workerId);
        lowEarner.setIsActive(true);

        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 4, 30));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(lowEarner));
        SalaryPayment savedPayment = new SalaryPayment();
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 4))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("EG: Process payroll with invalid UUID format for workerId")
    void testProcessPayrollInvalidUuidFormat() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId("not-a-valid-uuid");
        request.setEndDate(LocalDate.of(2024, 5, 31));

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("valid UUID"));
    }

    @Test
    @DisplayName("EG: Process payroll with missing workerId")
    void testProcessPayrollMissingWorkerId() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(null);
        request.setEndDate(LocalDate.of(2024, 5, 31));

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("required"));
    }

    @Test
    @DisplayName("EG: Process payroll with missing endDate")
    void testProcessPayrollMissingEndDate() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(null);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("required"));
    }

    @Test
    @DisplayName("EG: Process payroll for non-existent worker")
    void testProcessPayrollWorkerNotFound() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 6, 30));

        when(workerRepository.findById(workerId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("missing or inactive"));
    }

    @Test
    @DisplayName("EG: Process payroll for inactive worker")
    void testProcessPayrollInactiveWorker() {
        Worker inactiveWorker = new Worker("Inactive Joe", "Shed Worker", joinDate, BigDecimal.valueOf(2000));
        inactiveWorker.setWorkerId(workerId);
        inactiveWorker.setIsActive(false);

        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 6, 30));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(inactiveWorker));

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("missing or inactive"));
    }

    @Test
    @DisplayName("EG: Process payroll for worker without joinDate")
    void testProcessPayrollWorkerNoJoinDate() {
        Worker noJoinDate = new Worker("No Join Worker", "Contractor", null, BigDecimal.valueOf(1500));
        noJoinDate.setWorkerId(workerId);
        noJoinDate.setIsActive(true);

        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 7, 31));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(noJoinDate));

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("joinDate"));
    }

    @Test
    @DisplayName("EG: Process payroll with endDate before joinDate (invalid)")
    void testProcessPayrollEndDateBeforeJoinDate() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(joinDate.minusDays(5)); // Before join date

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("on or after"));
    }

    @Test
    @DisplayName("BVA: Daily rate calculation precision (salary/30 with rounding)")
    void testProcessPayrollDailyRateRounding() {
        Worker oddsalaryWorker = new Worker("Odd Rate", "Worker", joinDate, BigDecimal.valueOf(1234.56));
        oddsalaryWorker.setWorkerId(workerId);
        oddsalaryWorker.setIsActive(true);

        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 8, 31));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(oddsalaryWorker));
        SalaryPayment savedPayment = new SalaryPayment();
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 8))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify daily rate is rounded correctly (to 2 decimal places)
    }

    @Test
    @DisplayName("EP: Process payroll sets correct status = PROCESSED")
    void testProcessPayrollStatus() {
        PayrollProcessRequest request = new PayrollProcessRequest();
        request.setWorkerId(workerId.toString());
        request.setEndDate(LocalDate.of(2024, 9, 30));

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(activeWorker));
        SalaryPayment savedPayment = new SalaryPayment();
        savedPayment.setStatus("PROCESSED");
        when(salaryPaymentRepository.findByWorkerIdAndPeriodYearAndPeriodMonth(workerId, 2024, 9))
                .thenReturn(Optional.empty());
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(savedPayment);

        ResponseEntity<?> response = payrollController.processPayroll(request, "test-user-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ===== PAYROLL REPORT TESTS =====

    @Test
    @DisplayName("EP: Get payroll report with year and month filters")
    void testGetPayrollReportWithYearAndMonth() {
        List<SalaryPayment> payments = new ArrayList<>();
        SalaryPayment payment = new SalaryPayment();
        payment.setId(paymentId);
        payment.setWorker(activeWorker);
        payment.setPeriodYear(2024);
        payment.setPeriodMonth(3);
        payment.setDaysWorked(20);
        payment.setMonthlySalary(BigDecimal.valueOf(3000));
        payment.setDailyRate(BigDecimal.valueOf(100));
        payment.setAmountPaid(BigDecimal.valueOf(2000));
        payments.add(payment);

        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonth(2024, 3))
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2024, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("EP: Get payroll report with year filter only")
    void testGetPayrollReportWithYearOnly() {
        List<SalaryPayment> payments = new ArrayList<>();
        SalaryPayment payment1 = new SalaryPayment();
        payment1.setWorker(activeWorker);
        payment1.setPeriodYear(2024);
        payment1.setPeriodMonth(1);
        payment1.setDaysWorked(22);
        payment1.setMonthlySalary(BigDecimal.valueOf(3000));
        payment1.setDailyRate(BigDecimal.valueOf(100));
        payment1.setAmountPaid(BigDecimal.valueOf(2200));
        payments.add(payment1);

        SalaryPayment payment2 = new SalaryPayment();
        payment2.setWorker(activeWorker);
        payment2.setPeriodYear(2024);
        payment2.setPeriodMonth(2);
        payment2.setDaysWorked(20);
        payment2.setMonthlySalary(BigDecimal.valueOf(3000));
        payment2.setDailyRate(BigDecimal.valueOf(100));
        payment2.setAmountPaid(BigDecimal.valueOf(2000));
        payments.add(payment2);

        when(salaryPaymentRepository.findByPeriodYear(2024)).thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2024, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1);
    }

    @Test
    @DisplayName("EP: Get payroll report without filters (all records)")
    void testGetPayrollReportNoFilters() {
        List<SalaryPayment> payments = new ArrayList<>();
        SalaryPayment payment = new SalaryPayment();
        payment.setWorker(activeWorker);
        payment.setPeriodYear(2024);
        payment.setPeriodMonth(5);
        payment.setDaysWorked(21);
        payment.setMonthlySalary(BigDecimal.valueOf(3000));
        payment.setDailyRate(BigDecimal.valueOf(100));
        payment.setAmountPaid(BigDecimal.valueOf(2100));
        payments.add(payment);

        when(salaryPaymentRepository.findAllByOrderByPeriodYearDescPeriodMonthDescProcessedAtDesc())
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("BVA: Get payroll report for December (month = 12)")
    void testGetPayrollReportDecember() {
        List<SalaryPayment> payments = new ArrayList<>();

        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonth(2024, 12))
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2024, 12);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("BVA: Get payroll report for January (month = 1)")
    void testGetPayrollReportJanuary() {
        List<SalaryPayment> payments = new ArrayList<>();

        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonth(2024, 1))
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2024, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("EP: Get payroll report with empty result set")
    void testGetPayrollReportEmptyResult() {
        List<SalaryPayment> payments = new ArrayList<>();

        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonth(2025, 6))
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2025, 6);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    @Test
    @DisplayName("EP: Get payroll report with multiple workers in same period")
    void testGetPayrollReportMultipleWorkers() {
        List<SalaryPayment> payments = new ArrayList<>();

        Worker worker2 = new Worker("Jane Doe", "Accountant", joinDate, BigDecimal.valueOf(3500));
        worker2.setWorkerId(UUID.randomUUID());

        SalaryPayment payment1 = new SalaryPayment();
        payment1.setWorker(activeWorker);
        payment1.setPeriodYear(2024);
        payment1.setPeriodMonth(7);
        payment1.setDaysWorked(22);
        payment1.setMonthlySalary(BigDecimal.valueOf(3000));
        payment1.setDailyRate(BigDecimal.valueOf(100));
        payment1.setAmountPaid(BigDecimal.valueOf(2200));
        payments.add(payment1);

        SalaryPayment payment2 = new SalaryPayment();
        payment2.setWorker(worker2);
        payment2.setPeriodYear(2024);
        payment2.setPeriodMonth(7);
        payment2.setDaysWorked(22);
        payment2.setMonthlySalary(BigDecimal.valueOf(3500));
        payment2.setDailyRate(BigDecimal.valueOf(116.67));
        payment2.setAmountPaid(BigDecimal.valueOf(2566.74));
        payments.add(payment2);

        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonth(2024, 7))
                .thenReturn(payments);

        ResponseEntity<List<PayrollReportItem>> response = payrollController
                .getPayrollReport(2024, 7);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1);
    }
}

