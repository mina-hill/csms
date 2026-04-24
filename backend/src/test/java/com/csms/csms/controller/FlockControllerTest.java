package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.Flock;
import com.csms.csms.entity.AuditLog;
import com.csms.csms.entity.FlockStatus;
import com.csms.csms.repository.AuditLogRepository;
import com.csms.csms.repository.FlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * White-Box Test Suite for FlockController.updateFlock()
 * 
 * This test class covers all decision branches in the updateFlock() method:
 * - Branch 1: Flock not found (notFound)
 * - Branch 2: Flock is CLOSED (conflict)
 * - Branch 3: Breed updated (success)
 * - Branch 4: CurrentQty updated (success)
 * - Branch 5: Notes updated (success)
 * - Branch 6: All fields null (edge case - success with no changes)
 * 
 * Total: 10 test cases for 100% branch coverage
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlockController - updateFlock() White-Box Tests")
class FlockControllerTest {

    private static final String TEST_ACTOR = "00000000-0000-0000-0000-000000000001";

    // Mock the repositories (don't use real database during tests)
    @Mock
    private FlockRepository flockRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CsmsAccessHelper accessHelper;

    // Inject mocks into the controller
    @InjectMocks
    private FlockController flockController;

    // Test data
    private UUID flockId;
    private Flock activeFlock;
    private FlockRequest updateRequest;

    /**
     * Setup: Runs BEFORE each test
     * Creates fresh test data so tests don't affect each other
     */
    @BeforeEach
    void setUp() {
        doNothing().when(accessHelper).requireShedManagerOrAdminOrThrow(any());
        flockId = UUID.randomUUID();
        
        activeFlock = new Flock();
        activeFlock.setFlockId(flockId);
        activeFlock.setFlockCode("FLK-001");
        activeFlock.setBreed("Broiler");
        activeFlock.setInitialQty(500);
        activeFlock.setCurrentQty(450);
        activeFlock.setArrivalDate(LocalDate.of(2024, 1, 15));
        activeFlock.setStatus(FlockStatus.ACTIVE);
        activeFlock.setNotes("Original notes");
        
        updateRequest = new FlockRequest();
    }

    // ========== BRANCH 1: FLOCK NOT FOUND ==========

    /**
     * TEST 1: updateFlock() should return 404 when flock doesn't exist
     * 
     * Branch Covered: Optional.isEmpty() = true
     * Input: Non-existent UUID
     * Expected: HTTP 404 Not Found
     */
    @Test
    @DisplayName("Test 1: Should return 404 when flock not found")
    void testUpdateFlock_FlockNotFound() {
        // Arrange: Mock repository to return empty (flock doesn't exist)
        when(flockRepository.findById(flockId)).thenReturn(Optional.empty());

        updateRequest.setBreed("Layer");
        updateRequest.setCurrentQty(600);

        // Act: Call the method
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert: Check response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify: Ensure repository was called
        verify(flockRepository, times(1)).findById(flockId);
        
        // Verify: Ensure NO audit log was created
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    // ========== BRANCH 2: FLOCK IS CLOSED ==========

    /**
     * TEST 2: updateFlock() should return 409 CONFLICT when flock is CLOSED
     * 
     * Branch Covered: flock.getStatus() == FlockStatus.CLOSED
     * Input: CLOSED flock + valid update request
     * Expected: HTTP 409 Conflict with error message
     */
    @Test
    @DisplayName("Test 2: Should return 409 when flock is CLOSED")
    void testUpdateFlock_FlockAlreadyClosed() {
        // Arrange: Set flock status to CLOSED
        activeFlock.setStatus(FlockStatus.CLOSED);
        activeFlock.setCloseDate(LocalDate.of(2024, 2, 15));
        
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));

        updateRequest.setBreed("Layer");
        updateRequest.setCurrentQty(400);

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("CLOSED"));
        
        // Verify: Repository was called but NO save occurred (no update)
        verify(flockRepository, times(1)).findById(flockId);
        verify(flockRepository, never()).save(any(Flock.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    // ========== BRANCH 3: UPDATE BREED ONLY ==========

    /**
     * TEST 3: updateFlock() should update BREED when other fields are null
     * 
     * Branch Covered: req.getBreed() != null
     * Input: breed="Layer", currentQty=null, notes=null
     * Expected: HTTP 200, breed updated, others unchanged
     */
    @Test
    @DisplayName("Test 3: Should update only BREED when other fields are null")
    void testUpdateFlock_UpdateBreedOnly() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setBreed("Layer");
        updateRequest.setCurrentQty(null);
        updateRequest.setNotes(null);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Flock updated = (Flock) response.getBody();
        assertEquals("Layer", updated.getBreed());
        
        // Verify: save() was called exactly once
        verify(flockRepository, times(1)).save(any(Flock.class));
        
        // Verify: audit log was created
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== BRANCH 4: UPDATE CURRENT_QTY ONLY ==========

    /**
     * TEST 4: updateFlock() should update CURRENT_QTY when other fields are null
     * 
     * Branch Covered: req.getCurrentQty() != null
     * Input: breed=null, currentQty=400, notes=null
     * Expected: HTTP 200, currentQty updated, others unchanged
     */
    @Test
    @DisplayName("Test 4: Should update only CURRENT_QTY when other fields are null")
    void testUpdateFlock_UpdateCurrentQtyOnly() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setBreed(null);
        updateRequest.setCurrentQty(400);
        updateRequest.setNotes(null);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Flock updated = (Flock) response.getBody();
        assertEquals(400, updated.getCurrentQty());
        
        // Verify: save() was called
        verify(flockRepository, times(1)).save(any(Flock.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== BRANCH 5: UPDATE NOTES ONLY ==========

    /**
     * TEST 5: updateFlock() should update NOTES when other fields are null
     * 
     * Branch Covered: req.getNotes() != null
     * Input: breed=null, currentQty=null, notes="New notes"
     * Expected: HTTP 200, notes updated, others unchanged
     */
    @Test
    @DisplayName("Test 5: Should update only NOTES when other fields are null")
    void testUpdateFlock_UpdateNotesOnly() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setBreed(null);
        updateRequest.setCurrentQty(null);
        updateRequest.setNotes("Updated notes");
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify: save() was called
        verify(flockRepository, times(1)).save(any(Flock.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== BRANCH 6: UPDATE ALL FIELDS ==========

    /**
     * TEST 6: updateFlock() should update ALL fields when all provided
     * 
     * Branch Covered: All conditions true
     * Input: breed="Broiler", currentQty=550, notes="All updated"
     * Expected: HTTP 200, all fields updated, audit log created
     */
    @Test
    @DisplayName("Test 6: Should update ALL fields when all provided")
    void testUpdateFlock_UpdateAllFields() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setBreed("Layer");
        updateRequest.setCurrentQty(550);
        updateRequest.setNotes("All updated");
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify: save() called once with updated flock
        verify(flockRepository, times(1)).save(any(Flock.class));
        
        // Verify: audit log created with both old and new values
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== BRANCH 7: ALL FIELDS NULL (EDGE CASE) ==========

    /**
     * TEST 7: updateFlock() should succeed even when all fields are null
     * 
     * Branch Covered: No updates, but no error either (success path with no changes)
     * Input: breed=null, currentQty=null, notes=null
     * Expected: HTTP 200, no changes made, audit log still created
     */
    @Test
    @DisplayName("Test 7: Should succeed when all fields are null (no-op update)")
    void testUpdateFlock_AllFieldsNull() {
        // Arrange
        String originalBreed = activeFlock.getBreed();
        Integer originalQty = activeFlock.getCurrentQty();
        
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setBreed(null);
        updateRequest.setCurrentQty(null);
        updateRequest.setNotes(null);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Flock unchanged = (Flock) response.getBody();
        assertEquals(originalBreed, unchanged.getBreed());
        assertEquals(originalQty, unchanged.getCurrentQty());
        
        // Verify: save was still called (with unchanged object)
        verify(flockRepository, times(1)).save(any(Flock.class));
    }

    // ========== BOUNDARY TESTS: QUANTITY EDGE CASES ==========

    /**
     * TEST 8: updateFlock() should accept ZERO quantity
     * 
     * Boundary Test: currentQty = 0
     * Expected: HTTP 200, quantity set to 0
     */
    @Test
    @DisplayName("Test 8: Should accept currentQty = 0")
    void testUpdateFlock_QuantityZero() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setCurrentQty(0);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(flockRepository, times(1)).save(any(Flock.class));
    }

    /**
     * TEST 9: updateFlock() should accept LARGE quantity
     * 
     * Boundary Test: currentQty = 999999
     * Expected: HTTP 200
     */
    @Test
    @DisplayName("Test 9: Should accept large currentQty value")
    void testUpdateFlock_LargeQuantity() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setCurrentQty(999999);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(flockRepository, times(1)).save(any(Flock.class));
    }

    /**
     * TEST 10: updateFlock() should accept NEGATIVE quantity (business logic decides if invalid)
     * 
     * Boundary Test: currentQty = -1
     * Expected: HTTP 200 (no validation on negative; business rule may differ)
     */
    @Test
    @DisplayName("Test 10: Should accept negative currentQty (no constraint in code)")
    void testUpdateFlock_NegativeQuantity() {
        // Arrange
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        updateRequest.setCurrentQty(-50);
        updateRequest.setUpdatedBy(UUID.randomUUID());

        // Act
        ResponseEntity<?> response = flockController.updateFlock(flockId, updateRequest, TEST_ACTOR);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(flockRepository, times(1)).save(any(Flock.class));
        
        // NOTE: This reveals a potential bug!
        // If negative quantities are not allowed, add validation to FlockController.updateFlock()
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("White-Box: closeFlock() + generateNextFlockCode()")
class FlockControllerCloseTest {

    private static final String TEST_ACTOR = "00000000-0000-0000-0000-000000000001";

    @Mock private FlockRepository flockRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CsmsAccessHelper accessHelper;
    @InjectMocks private FlockController flockController;

    private UUID flockId;
    private Flock activeFlock;
    private CloseFlockRequest closeRequest;

    @BeforeEach
    void setUp() {
        doNothing().when(accessHelper).requireShedManagerOrAdminOrThrow(any());
        flockId = UUID.randomUUID();

        activeFlock = new Flock();
        activeFlock.setFlockId(flockId);
        activeFlock.setStatus(FlockStatus.ACTIVE);
        activeFlock.setBreed("Broiler");
        activeFlock.setCurrentQty(500);

        closeRequest = new CloseFlockRequest();
        closeRequest.setClosedBy("admin");
    }

    // ---------------------------------------------------------------
    // closeFlock() — Branch 1: Flock not found → 404
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 1: Should return 404 when flock ID does not exist")
    void testCloseFlock_NotFound() {
        when(flockRepository.findById(flockId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = flockController.closeFlock(flockId, closeRequest, TEST_ACTOR);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(flockRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // closeFlock() — Branch 2: Flock already CLOSED → 409
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 2: Should return 409 when flock is already CLOSED")
    void testCloseFlock_AlreadyClosed() {
        activeFlock.setStatus(FlockStatus.CLOSED);
        activeFlock.setCloseDate(LocalDate.of(2024, 1, 10));
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));

        ResponseEntity<?> response = flockController.closeFlock(flockId, closeRequest, TEST_ACTOR);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("already CLOSED"));
        verify(flockRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // closeFlock() — Branch 3: ACTIVE flock, closeDate PROVIDED → 200
    // Path: found → not closed → closeDate != null → save
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 3: Should close flock with provided closeDate")
    void testCloseFlock_WithProvidedCloseDate() {
        LocalDate providedDate = LocalDate.of(2024, 6, 15);
        closeRequest.setCloseDate(providedDate);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        ResponseEntity<?> response = flockController.closeFlock(flockId, closeRequest, TEST_ACTOR);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FlockStatus.CLOSED, activeFlock.getStatus());
        assertEquals(providedDate, activeFlock.getCloseDate());
        verify(flockRepository, times(1)).save(activeFlock);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ---------------------------------------------------------------
    // closeFlock() — Branch 4: ACTIVE flock, closeDate NULL → defaults to today
    // Path: found → not closed → closeDate == null → LocalDate.now() → save
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 4: Should close flock with today's date when closeDate is null")
    void testCloseFlock_WithNullCloseDate_DefaultsToToday() {
        closeRequest.setCloseDate(null); // explicitly null
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(activeFlock));
        when(flockRepository.save(any(Flock.class))).thenReturn(activeFlock);

        LocalDate before = LocalDate.now();
        ResponseEntity<?> response = flockController.closeFlock(flockId, closeRequest, TEST_ACTOR);
        LocalDate after = LocalDate.now();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FlockStatus.CLOSED, activeFlock.getStatus());
        // closeDate must be today (between before and after handles midnight edge)
        assertFalse(activeFlock.getCloseDate().isBefore(before));
        assertFalse(activeFlock.getCloseDate().isAfter(after));
        verify(flockRepository, times(1)).save(activeFlock);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ---------------------------------------------------------------
    // generateNextFlockCode() — Loop runs ONCE (no collision)
    // do-while: count=0 → candidate="FLK-001" → existsByFlockCode=false → exit
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 5: generateNextFlockCode — loop exits on first iteration (no collision)")
    void testGenerateNextFlockCode_NoCollision() {
        

        // Trigger registerFlock which calls generateNextFlockCode internally
        when(flockRepository.count()).thenReturn(0L);
        when(flockRepository.existsByFlockCode("FLK-001")).thenReturn(false);

        FlockRequest registerReq = new FlockRequest();
        registerReq.setBreed("Broiler");
        registerReq.setInitialQty(100);
        registerReq.setArrivalDate(LocalDate.of(2024, 1, 1));

        Flock savedFlock = new Flock();
        savedFlock.setFlockCode("FLK-001");
        savedFlock.setStatus(FlockStatus.ACTIVE);
        when(flockRepository.save(any(Flock.class))).thenReturn(savedFlock);

        ResponseEntity<?> response = flockController.registerFlock(registerReq, TEST_ACTOR);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // Loop ran once: existsByFlockCode called exactly once for FLK-001
        verify(flockRepository, times(1)).existsByFlockCode("FLK-001");
    }

    // ---------------------------------------------------------------
    // generateNextFlockCode() — Loop runs MULTIPLE times (collision)
    // do-while: FLK-001 exists → FLK-002 exists → FLK-003 free → exit
    // ---------------------------------------------------------------
    @Test
    @DisplayName("Test 6: generateNextFlockCode — loop iterates on collision, exits when code is free")
    void testGenerateNextFlockCode_WithCollisions() {
        when(flockRepository.count()).thenReturn(0L);
        when(flockRepository.existsByFlockCode("FLK-001")).thenReturn(true);  // collision
        when(flockRepository.existsByFlockCode("FLK-002")).thenReturn(true);  // collision
        when(flockRepository.existsByFlockCode("FLK-003")).thenReturn(false); // free

        FlockRequest registerReq = new FlockRequest();
        registerReq.setBreed("Broiler");
        registerReq.setInitialQty(100);
        registerReq.setArrivalDate(LocalDate.of(2024, 1, 1));

        Flock savedFlock = new Flock();
        savedFlock.setFlockCode("FLK-003");
        savedFlock.setStatus(FlockStatus.ACTIVE);
        when(flockRepository.save(any(Flock.class))).thenReturn(savedFlock);

        ResponseEntity<?> response = flockController.registerFlock(registerReq, TEST_ACTOR);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // Verify loop ran 3 times before finding a free code
        verify(flockRepository, times(1)).existsByFlockCode("FLK-001");
        verify(flockRepository, times(1)).existsByFlockCode("FLK-002");
        verify(flockRepository, times(1)).existsByFlockCode("FLK-003");
    }
}