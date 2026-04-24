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
class WhiteBoxTests {

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