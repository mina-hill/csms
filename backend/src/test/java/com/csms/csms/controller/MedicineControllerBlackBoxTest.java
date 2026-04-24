package com.csms.csms.controller;

import com.csms.csms.entity.Medicine;
import com.csms.csms.entity.MedicinePurchase;
import com.csms.csms.entity.MedicineUsage;
import com.csms.csms.entity.Supplier;
import com.csms.csms.repository.MedicinePurchaseRepository;
import com.csms.csms.repository.MedicineRepository;
import com.csms.csms.repository.MedicineUsageRepository;
import com.csms.csms.repository.SupplierRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Black-Box Test Suite for MedicineController
 * Uses Equivalence Partitioning (EP), Boundary Value Analysis (BVA), and Error Guessing (EG).
 * 
 * Test Modules:
 * 1. Medicine Upsert: EP (new/existing), BVA (name edge cases, threshold bounds)
 * 2. Stock Adjustment: EP (increment/decrement), BVA (delta=0, negative result)
 * 3. Purchase Creation: EP (valid/invalid inputs), BVA (quantity=0, cost boundaries)
 * 4. Usage Creation: EP (valid/invalid dosages), BVA (dosage=0, future dates)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicineController - Black-Box Tests")
class MedicineControllerBlackBoxTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicinePurchaseRepository medicinePurchaseRepository;

    @Mock
    private MedicineUsageRepository medicineUsageRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private MedicineController medicineController;

    private UUID medicineId;
    private UUID supplierId;
    private UUID flockId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        medicineId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        flockId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ===== MEDICINE UPSERT TESTS =====

    @Test
    @DisplayName("EP: Upsert new medicine with valid name")
    void testUpsertNewMedicineSuccess() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName("Amoxicillin");
        request.setMinThreshold(10);

        when(medicineRepository.findByName("Amoxicillin")).thenReturn(Optional.empty());
        Medicine savedMedicine = new Medicine("Amoxicillin", 10);
        savedMedicine.setMedicineId(medicineId);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(savedMedicine);

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(medicineRepository, times(1)).save(any(Medicine.class));
    }

    @Test
    @DisplayName("EP: Upsert existing medicine updates threshold")
    void testUpsertExistingMedicineUpdatesThreshold() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName("Paracetamol");
        request.setMinThreshold(15);

        Medicine existing = new Medicine("Paracetamol", 5);
        existing.setMedicineId(medicineId);
        when(medicineRepository.findByName("Paracetamol")).thenReturn(Optional.of(existing));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(existing);

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(15, existing.getMinThreshold());
    }

    @Test
    @DisplayName("BVA: Upsert with blank name (null)")
    void testUpsertBlankName() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName(null);

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("name is required"));
    }

    @Test
    @DisplayName("BVA: Upsert with whitespace-only name")
    void testUpsertWhitespaceName() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName("   ");

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Upsert threshold = 0 (boundary)")
    void testUpsertThresholdZero() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName("Ibuprofen");
        request.setMinThreshold(0);

        when(medicineRepository.findByName("Ibuprofen")).thenReturn(Optional.empty());
        Medicine savedMedicine = new Medicine("Ibuprofen", 0);
        savedMedicine.setMedicineId(medicineId);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(savedMedicine);

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Upsert negative threshold coerced to 0")
    void testUpsertNegativeThresholdCoercedToZero() {
        MedicineUpsertRequest request = new MedicineUpsertRequest();
        request.setName("Aspirin");
        request.setMinThreshold(-5);

        when(medicineRepository.findByName("Aspirin")).thenReturn(Optional.empty());
        Medicine savedMedicine = new Medicine("Aspirin", 0);
        savedMedicine.setMedicineId(medicineId);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(savedMedicine);

        ResponseEntity<?> response = medicineController.upsertMedicine(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ===== STOCK ADJUSTMENT TESTS =====

    @Test
    @DisplayName("EP: Adjust stock - increment existing medicine")
    void testAdjustStockIncrement() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("Ciprofloxacin");
        request.setDelta(5);

        Medicine medicine = new Medicine("Ciprofloxacin");
        medicine.setMedicineId(medicineId);
        medicine.setCurrentStock(10);
        when(medicineRepository.findByName("Ciprofloxacin")).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicine);

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(15, medicine.getCurrentStock());
    }

    @Test
    @DisplayName("EP: Adjust stock - decrement existing medicine")
    void testAdjustStockDecrement() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("Tetracycline");
        request.setDelta(-3);

        Medicine medicine = new Medicine("Tetracycline");
        medicine.setMedicineId(medicineId);
        medicine.setCurrentStock(10);
        when(medicineRepository.findByName("Tetracycline")).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicine);

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7, medicine.getCurrentStock());
    }

    @Test
    @DisplayName("BVA: Adjust stock - delta = 0 (zero change not allowed)")
    void testAdjustStockDeltaZero() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("Metronidazole");
        request.setDelta(0);

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("delta must be non-zero"));
    }

    @Test
    @DisplayName("BVA: Adjust stock - negative delta results in negative stock (error)")
    void testAdjustStockNegativeResult() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("Doxycycline");
        request.setDelta(-15);

        Medicine medicine = new Medicine("Doxycycline");
        medicine.setCurrentStock(10);
        when(medicineRepository.findByName("Doxycycline")).thenReturn(Optional.of(medicine));

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Insufficient medicine stock"));
    }

    @Test
    @DisplayName("EG: Adjust stock - blank name")
    void testAdjustStockBlankName() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("");
        request.setDelta(5);

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("EG: Adjust stock - null delta")
    void testAdjustStockNullDelta() {
        MedicineStockAdjustRequest request = new MedicineStockAdjustRequest();
        request.setName("Clarithromycin");
        request.setDelta(null);

        ResponseEntity<?> response = medicineController.adjustStock(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ===== LOW STOCK TESTS =====

    @Test
    @DisplayName("EP: Get low-stock medicines with custom threshold")
    void testGetLowStockWithCustomThreshold() {
        List<Medicine> lowStockMeds = new ArrayList<>();
        Medicine med1 = new Medicine("Medicine1", 5);
        med1.setCurrentStock(2);
        lowStockMeds.add(med1);

        when(medicineRepository.findByCurrentStockLessThan(3)).thenReturn(lowStockMeds);

        ResponseEntity<List<Medicine>> response = medicineController.getLowStockMedicines(3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("EP: Get low-stock medicines using min threshold")
    void testGetLowStockUsingMinThreshold() {
        List<Medicine> lowStockMeds = new ArrayList<>();
        when(medicineRepository.findByCurrentStockLessThanMinThreshold()).thenReturn(lowStockMeds);

        ResponseEntity<List<Medicine>> response = medicineController.getLowStockMedicines(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(medicineRepository, times(1)).findByCurrentStockLessThanMinThreshold();
    }

    // ===== MEDICINE PURCHASE TESTS =====

    @Test
    @DisplayName("EP: Create purchase with valid medicine ID and supplier")
    void testCreatePurchaseValidMedicineAndSupplier() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(100);
        request.setUnitCost(BigDecimal.valueOf(5.50));

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(new Medicine("Test")));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(new Supplier()));
        when(medicinePurchaseRepository.save(any(MedicinePurchase.class)))
                .thenReturn(new MedicinePurchase());

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(medicinePurchaseRepository, times(1)).save(any(MedicinePurchase.class));
    }

    @Test
    @DisplayName("EP: Create purchase with medicine name (no ID)")
    void testCreatePurchaseWithMedicineNameLookup() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineName("Streptomycin");
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(50);
        request.setUnitCost(BigDecimal.valueOf(3.25));

        Medicine medicine = new Medicine("Streptomycin");
        medicine.setMedicineId(medicineId);
        when(medicineRepository.findByName("Streptomycin")).thenReturn(Optional.of(medicine));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(new Supplier()));
        when(medicinePurchaseRepository.save(any(MedicinePurchase.class)))
                .thenReturn(new MedicinePurchase());

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Create purchase - quantity = 1 (boundary)")
    void testCreatePurchaseQuantityOne() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(1);
        request.setUnitCost(BigDecimal.valueOf(10));

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(new Medicine("Test")));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(new Supplier()));
        when(medicinePurchaseRepository.save(any(MedicinePurchase.class)))
                .thenReturn(new MedicinePurchase());

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("BVA: Create purchase - quantity = 0 (invalid)")
    void testCreatePurchaseQuantityZero() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(0);
        request.setUnitCost(BigDecimal.valueOf(10));

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("quantity must be a positive integer"));
    }

    @Test
    @DisplayName("BVA: Create purchase - unitCost = 0 (invalid)")
    void testCreatePurchaseUnitCostZero() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(10);
        request.setUnitCost(BigDecimal.ZERO);

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("unitCost must be positive"));
    }

    @Test
    @DisplayName("EG: Create purchase - missing supplier ID")
    void testCreatePurchaseMissingSupplier() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(null);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(10);
        request.setUnitCost(BigDecimal.valueOf(5));

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("supplierId is required"));
    }

    @Test
    @DisplayName("EG: Create purchase - medicine not found")
    void testCreatePurchaseMedicineNotFound() {
        MedicinePurchaseRequest request = new MedicinePurchaseRequest();
        request.setMedicineId(medicineId);
        request.setSupplierId(supplierId);
        request.setPurchaseDate(LocalDate.now());
        request.setQuantity(10);
        request.setUnitCost(BigDecimal.valueOf(5));

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = medicineController.createPurchase(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Medicine not found"));
    }

    // ===== MEDICINE USAGE TESTS =====

    @Test
    @DisplayName("EP: Create usage with valid flock, medicine, and dosage")
    void testCreateUsageSuccess() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(flockId);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.valueOf(25.5));
        request.setUsageTime("08:00");

        when(medicineUsageRepository.save(any(MedicineUsage.class))).thenReturn(new MedicineUsage());

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(medicineUsageRepository, times(1)).save(any(MedicineUsage.class));
    }

    @Test
    @DisplayName("BVA: Create usage - dosage = 0 (invalid)")
    void testCreateUsageDosageZero() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(flockId);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.ZERO);
        request.setUsageTime("08:00");

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("dosage must be positive"));
    }

    @Test
    @DisplayName("BVA: Create usage - negative dosage (invalid)")
    void testCreateUsageNegativeDosage() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(flockId);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.valueOf(-10));
        request.setUsageTime("08:00");

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("EG: Create usage - missing usage time")
    void testCreateUsageMissingUsageTime() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(flockId);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.valueOf(15));
        request.setUsageTime(null);

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("usageTime is required"));
    }

    @Test
    @DisplayName("EG: Create usage - blank usage time")
    void testCreateUsageBlankUsageTime() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(flockId);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.valueOf(15));
        request.setUsageTime("   ");

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("EG: Create usage - missing flock ID")
    void testCreateUsageMissingFlockId() {
        MedicineUsageRequest request = new MedicineUsageRequest();
        request.setFlockId(null);
        request.setMedicineId(medicineId);
        request.setUsageDate(LocalDate.now());
        request.setDosage(BigDecimal.valueOf(15));
        request.setUsageTime("08:00");

        ResponseEntity<?> response = medicineController.createUsage(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
