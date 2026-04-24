//package com.csms.csms.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * Black-Box Test Suite — Flock Management Module
// *
// * Techniques applied:
// *   EP  = Equivalence Partitioning
// *   BVA = Boundary Value Analysis
// *   EG  = Error Guessing
// *
// * Endpoints under test:
// *   POST  /api/flocks            – register flock
// *   PUT   /api/flocks/{id}       – update active flock
// *   PATCH /api/flocks/{id}/close – close flock
// *   GET   /api/flocks            – list all flocks
// *   GET   /api/flocks/{id}/audit – audit history
// *
// * Design notes (derived from actual FlockController source):
// *
// *   1. registerFlock() has NO explicit null/empty validation guards.
// *      Missing or invalid fields propagate to the DB and cause a
// *      constraint violation (>=400). Tests for invalid inputs assert
// *      status >= 400 rather than strictly 400.
// *
// *   2. closeFlock() defaults closeDate to LocalDate.now() when the
// *      field is absent. An empty body {} is therefore VALID and
// *      returns 200. There is also no before-arrival-date guard.
// *
// *   3. GET /api/flocks/{id}/audit returns an empty 200 list for an
// *      unknown UUID — it never returns 404.
// *
// *   4. updateFlock() is a partial update: null fields in the request
// *      body are silently skipped. The field name for quantity in
// *      updates is currentQty (not initialQty).
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//@DisplayName("Flock Management — Black-Box Tests")
//class FlockControllerBlackBoxTest {
//
//    @Autowired MockMvc mvc;`n    @org.springframework.boot.test.mock.mockito.MockBean`n    private com.csms.csms.auth.CsmsAccessHelper accessHelper;
//    @Autowired ObjectMapper om;`n    @org.junit.jupiter.api.BeforeEach`n    void setUp() {`n        org.mockito.Mockito.doNothing().when(accessHelper).requireShedManagerOrAdminOrThrow(org.mockito.ArgumentMatchers.anyString());`n    }
//
//    // ─────────────────────────────────────────────
//    //  Helper
//    // ─────────────────────────────────────────────
//
//    /** Creates a minimal valid flock and returns its flockId UUID string. */
//    private String registerFlock(String breed, int qty, String arrivalDate) throws Exception {
//        Map<String, Object> body = new HashMap<>();
//        body.put("breed",       breed);
//        body.put("initialQty",  qty);
//        body.put("arrivalDate", arrivalDate);
//
//        String resp = mvc.perform(post("/api/flocks")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(om.writeValueAsString(body)))
//                .andExpect(status().isCreated())
//                .andReturn().getResponse().getContentAsString();
//
//        return om.readTree(resp).get("flockId").asText();
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  1. POST /api/flocks — Register Flock
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-001 Register Flock")
//    class RegisterFlock {
//
//        // ── EP: Valid Equivalence Classes ────────────────────────────
//
//        @Test
//        @DisplayName("EP-VEC-01 | Valid payload → 201, flockId + flockCode generated, status ACTIVE")
//        void validPayloadReturnsCreated() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Ross 308");
//            body.put("initialQty",  5000);
//            body.put("arrivalDate", "2025-01-15");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.flockId").isNotEmpty())
//                    .andExpect(jsonPath("$.flockCode").isNotEmpty())
//                    .andExpect(jsonPath("$.status").value("ACTIVE"));
//        }
//
//        @Test
//        @DisplayName("EP-VEC-02 | Optional fields omitted (no supplierId, no notes) → 201 Created")
//        void optionalFieldsOmitted() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Cobb 500");
//            body.put("initialQty",  1000);
//            body.put("arrivalDate", LocalDate.now().toString());
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.status").value("ACTIVE"));
//        }
//
//        // ── EP: Invalid Equivalence Classes ──────────────────────────
//
//        @Test
//        @DisplayName("EP-IEC-01 | Missing breed → non-2xx (DB NOT NULL constraint)")
//        void missingBreedRejected() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("initialQty",  1000);
//            body.put("arrivalDate", "2025-01-15");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(result ->
//                            org.junit.jupiter.api.Assertions.assertTrue(
//                                    result.getResponse().getStatus() >= 400,
//                                    "Expected >=400 for missing breed"));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-02 | Missing arrivalDate → non-2xx (DB NOT NULL constraint)")
//        void missingArrivalDateRejected() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",      "Ross 308");
//            body.put("initialQty", 1000);
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(result ->
//                            org.junit.jupiter.api.Assertions.assertTrue(
//                                    result.getResponse().getStatus() >= 400,
//                                    "Expected >=400 for missing arrivalDate"));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-03 | initialQty = 0 → non-2xx (DB CHECK initial_qty > 0)")
//        void zeroQuantityRejected() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Ross 308");
//            body.put("initialQty",  0);
//            body.put("arrivalDate", "2025-01-15");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(result ->
//                            org.junit.jupiter.api.Assertions.assertTrue(
//                                    result.getResponse().getStatus() >= 400,
//                                    "Expected >=400 for zero initialQty"));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-04 | Negative initialQty → non-2xx (DB CHECK initial_qty > 0)")
//        void negativeQuantityRejected() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Ross 308");
//            body.put("initialQty",  -1);
//            body.put("arrivalDate", "2025-01-15");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(result ->
//                            org.junit.jupiter.api.Assertions.assertTrue(
//                                    result.getResponse().getStatus() >= 400,
//                                    "Expected >=400 for negative initialQty"));
//        }
//
//        // ── BVA ──────────────────────────────────────────────────────
//
//        @Test
//        @DisplayName("BVA-01 | initialQty = 1 (lower boundary, just above 0) → 201 Created")
//        void minQtyOneBoundary() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Test Breed");
//            body.put("initialQty",  1);
//            body.put("arrivalDate", "2025-06-01");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @DisplayName("BVA-02 | initialQty = 100 000 (large upper value) → 201 Created")
//        void largeQtyAccepted() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Large Batch");
//            body.put("initialQty",  100_000);
//            body.put("arrivalDate", "2025-06-01");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated());
//        }
//
//        // ── EG ───────────────────────────────────────────────────────
//
//        @Test
//        @DisplayName("EG-01 | Empty string breed → non-2xx (DB rejects empty string for NOT NULL VARCHAR)")
//        void emptyBreedStringRejected() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "");
//            body.put("initialQty",  500);
//            body.put("arrivalDate", "2025-01-15");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(result ->
//                            org.junit.jupiter.api.Assertions.assertTrue(
//                                    result.getResponse().getStatus() >= 400,
//                                    "Expected >=400 for empty breed string"));
//        }
//
//        @Test
//        @DisplayName("EG-02 | Future arrivalDate → 201 Created (no future-date restriction on arrival)")
//        void futureArrivalDateAccepted() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Future Breed");
//            body.put("initialQty",  100);
//            body.put("arrivalDate", LocalDate.now().plusDays(30).toString());
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @DisplayName("EG-03 | Same breed registered twice → both succeed (no uniqueness constraint on breed)")
//        void duplicateBreedAllowed() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("breed",       "Ross 308");
//            body.put("initialQty",  1000);
//            body.put("arrivalDate", "2025-01-01");
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated());
//
//            mvc.perform(post("/api/flocks")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isCreated());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  2. PUT /api/flocks/{id} — Update Flock
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-002 Update Flock")
//    class UpdateFlock {
//
//        @Test
//        @DisplayName("EP-VEC-03 | Update breed on ACTIVE flock → 200 OK, breed reflected in response")
//        void updateActiveFlockBreed() throws Exception {
//            String id = registerFlock("Old Breed", 1000, "2025-01-01");
//
//            // Use currentQty (not initialQty) — controller maps to flock.currentQty
//            Map<String, Object> update = new HashMap<>();
//            update.put("breed",      "New Breed");
//            update.put("currentQty", 950);
//
//            mvc.perform(put("/api/flocks/" + id)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(update)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.breed").value("New Breed"));
//        }
//
//        @Test
//        @DisplayName("EP-VEC-04 | Partial update (notes only) on ACTIVE flock → 200 OK")
//        void partialUpdateNotesOnly() throws Exception {
//            String id = registerFlock("Ross 308", 500, "2025-02-01");
//
//            Map<String, Object> update = new HashMap<>();
//            update.put("notes", "Updated batch reference");
//
//            mvc.perform(put("/api/flocks/" + id)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(update)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.notes").value("Updated batch reference"));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-05 | Update CLOSED flock → 409 Conflict")
//        void updateClosedFlockRejected() throws Exception {
//            String id = registerFlock("Closed Breed", 500, "2024-01-01");
//
//            // Close the flock (empty body → defaults to today)
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content("{}"))
//                    .andExpect(status().isOk());
//
//            Map<String, Object> update = new HashMap<>();
//            update.put("breed",      "Should Fail");
//            update.put("currentQty", 100);
//
//            mvc.perform(put("/api/flocks/" + id)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(update)))
//                    .andExpect(status().isConflict());
//        }
//
//        @Test
//        @DisplayName("EG-04 | Update non-existent UUID → 404 Not Found")
//        void updateNonExistentFlock() throws Exception {
//            Map<String, Object> update = new HashMap<>();
//            update.put("breed",      "Ghost");
//            update.put("currentQty", 100);
//
//            mvc.perform(put("/api/flocks/00000000-0000-0000-0000-000000000000")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(update)))
//                    .andExpect(status().isNotFound());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  3. PATCH /api/flocks/{id}/close — Close Flock
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-003 Close Flock")
//    class CloseFlock {
//
//        @Test
//        @DisplayName("EP-VEC-05 | Close ACTIVE flock with explicit closeDate → 200 OK, status CLOSED")
//        void closeActiveFlockExplicitDate() throws Exception {
//            String id = registerFlock("Finisher", 2000, "2025-01-01");
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("closeDate", "2025-03-01");
//
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.status").value("CLOSED"))
//                    .andExpect(jsonPath("$.closeDate").value("2025-03-01"));
//        }
//
//        @Test
//        @DisplayName("EP-VEC-06 | Close ACTIVE flock with no closeDate → 200 OK, closeDate defaults to today")
//        void closeActiveFlockNoDateDefaultsToToday() throws Exception {
//            String id = registerFlock("Auto Date", 500, "2024-01-01");
//
//            // Empty body — controller falls back to LocalDate.now()
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content("{}"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.status").value("CLOSED"))
//                    .andExpect(jsonPath("$.closeDate").value(LocalDate.now().toString()));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-06 | Close already-CLOSED flock → 409 Conflict")
//        void closeAlreadyClosedFlock() throws Exception {
//            String id = registerFlock("Double Close", 300, "2025-01-01");
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("closeDate", "2025-04-01");
//
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isOk());
//
//            // Second close attempt — must be rejected
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isConflict());
//        }
//
//        @Test
//        @DisplayName("BVA-03 | closeDate = arrivalDate (same day) → 200 OK")
//        void closeDateEqualToArrivalDate() throws Exception {
//            String id = registerFlock("Same Day", 100, "2025-05-01");
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("closeDate", "2025-05-01");
//
//            mvc.perform(patch("/api/flocks/" + id + "/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isOk());
//        }
//
//        @Test
//        @DisplayName("EG-05 | Close non-existent UUID → 404 Not Found")
//        void closeNonExistentFlock() throws Exception {
//            Map<String, Object> body = new HashMap<>();
//            body.put("closeDate", "2025-04-01");
//
//            mvc.perform(patch("/api/flocks/00000000-0000-0000-0000-000000000000/close")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(om.writeValueAsString(body)))
//                    .andExpect(status().isNotFound());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  4. GET /api/flocks — List All Flocks
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("List Flocks")
//    class ListFlocks {
//
//        @Test
//        @DisplayName("EP-VEC-07 | GET /api/flocks → 200 OK, array body")
//        void listFlocks() throws Exception {
//            mvc.perform(get("/api/flocks"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray());
//        }
//
//        @Test
//        @DisplayName("EP-VEC-08 | Registered flock appears in list")
//        void registeredFlockAppearsInList() throws Exception {
//            registerFlock("Listed Breed", 750, "2025-03-01");
//
//            mvc.perform(get("/api/flocks"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$[?(@.breed == 'Listed Breed')]").exists());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  5. GET /api/flocks/{id}/audit — Audit History
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("Flock Audit History")
//    class AuditHistory {
//
//        @Test
//        @DisplayName("EP-VEC-09 | Audit history for valid flock → 200 OK, array body")
//        void auditForExistingFlock() throws Exception {
//            String id = registerFlock("Audit Breed", 200, "2025-01-01");
//
//            mvc.perform(get("/api/flocks/" + id + "/audit"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray());
//        }
//
//        @Test
//        @DisplayName("EP-VEC-10 | Audit contains a FLOCK_CREATED entry after registration")
//        void auditContainsCreatedEntry() throws Exception {
//            String id = registerFlock("Audit Entry Breed", 300, "2025-01-01");
//
//            mvc.perform(get("/api/flocks/" + id + "/audit"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$[?(@.action == 'FLOCK_CREATED')]").exists());
//        }
//
//        @Test
//        @DisplayName("EG-06 | Audit for unknown UUID → 200 OK with empty array (controller has no 404 guard)")
//        void auditForUnknownFlockReturnsEmptyList() throws Exception {
//            // auditLogRepository.findByFlockIdOrderByLoggedAtDesc() returns []
//            // for any unknown ID; the controller wraps it in 200 directly.
//            mvc.perform(get("/api/flocks/00000000-0000-0000-0000-000000000000/audit"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray())
//                    .andExpect(jsonPath("$.length()").value(0));
//        }
//    }
//}

package com.csms.csms.controller;
import com.csms.csms.auth.CsmsAccessHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Black-Box Test Suite — Flock Management Module
 *
 * Techniques applied:
 *   EP  = Equivalence Partitioning
 *   BVA = Boundary Value Analysis
 *   EG  = Error Guessing
 *
 * Endpoints under test:
 *   POST  /api/flocks            – register flock
 *   PUT   /api/flocks/{id}       – update active flock
 *   PATCH /api/flocks/{id}/close – close flock
 *   GET   /api/flocks            – list all flocks
 *   GET   /api/flocks/{id}/audit – audit history
 *
 * ── Changes from previous version ───────────────────────────────────────
 *
 *  [FIX-1]  Auth header added everywhere.
 *           FlockController now calls accessHelper.requireShedManagerOrAdminOrThrow(actorId)
 *           on POST, PUT, and PATCH. Without the X-User-Id header the helper
 *           throws / returns 403, making every write test fail.
 *           Fix: every mutating request now sends the header via VALID_ACTOR_HEADER.
 *           GET endpoints do NOT have the auth guard, so no header is needed there.
 *
 *  [FIX-2]  registerFlock() helper now includes the auth header.
 *           The helper is used by most tests to set up state; without the
 *           header it was itself returning 403 and the returned flockId was
 *           blank/null, causing downstream NullPointerExceptions or 400s.
 *
 *  [FIX-3]  Bean Validation (jakarta) is active on registerFlock().
 *           The @Valid annotation on the @RequestBody means Spring validates
 *           @NotBlank(breed), @NotNull+@Min(1)(initialQty), @NotNull(arrivalDate)
 *           BEFORE the controller body runs, returning 400 immediately.
 *           Test comments updated from "DB constraint" → "Bean Validation".
 *
 *  [FIX-4]  EG-01 (empty breed) now reliably expects 400.
 *           @NotBlank rejects "" at the validation layer; the comment no longer
 *           implies this reaches the database.
 *
 *  [FIX-5]  updateFlock() does NOT have @Valid, so null/empty breed on a PUT
 *           is silently skipped (partial-update semantics). The update tests
 *           already reflect this; no change needed there — just confirmed.
 *
 * ── Assumptions ─────────────────────────────────────────────────────────
 *
 *  • CsmsAccessHelper.USER_ID_HEADER = "X-User-Id"
 *    (used as the header name). If your constant differs, update AUTH_HEADER.
 *
 *  • The helper accepts ANY non-null user ID in the test/dev profile, or
 *    the DB already has a user row matching TEST_USER_ID. If the helper
 *    validates the ID against the users table, insert that row in a @BeforeEach
 *    or use @Sql to seed it. If it only checks the header is present, any UUID works.
 *
 *  • Tests run with @Transactional so every test rolls back — no state leaks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Flock Management — Black-Box Tests")
class FlockControllerBlackBoxTest {

    @Autowired MockMvc mvc;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.csms.csms.auth.CsmsAccessHelper accessHelper;
    @Autowired ObjectMapper om;
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.doAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg == null || arg.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid session");
            }
            return null;
        }).when(accessHelper).requireShedManagerOrAdminOrThrow(org.mockito.ArgumentMatchers.nullable(String.class));
    }
    // ─────────────────────────────────────────────
    //  Auth constants  [FIX-1]
    // ─────────────────────────────────────────────

    /** Must match CsmsAccessHelper.USER_ID_HEADER */
    private static final String AUTH_HEADER = CsmsAccessHelper.USER_ID_HEADER;

    /**
     * A UUID that CsmsAccessHelper will accept as a valid shed-manager/admin.
     * If the helper only checks presence, any UUID string works.
     * If it queries the DB, make sure this user exists (seed via @Sql or @BeforeEach).
     */
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    // ─────────────────────────────────────────────
    //  Helper  [FIX-2]
    // ─────────────────────────────────────────────

    /**
     * Creates a minimal valid flock and returns its flockId UUID string.
     * Auth header is now included so the POST succeeds.
     */
    private String registerFlock(String breed, int qty, String arrivalDate) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("breed",       breed);
        body.put("initialQty",  qty);
        body.put("arrivalDate", arrivalDate);

        String resp = mvc.perform(post("/api/flocks")
                        .header(AUTH_HEADER, TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return om.readTree(resp).get("flockId").asText();
    }

    // ─────────────────────────────────────────────────────────────────
    //  1. POST /api/flocks — Register Flock
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-001 Register Flock")
    class RegisterFlock {

        // ── EP: Valid Equivalence Classes ────────────────────────────

        @Test
        @DisplayName("EP-VEC-01 | Valid payload → 201, flockId + flockCode generated, status ACTIVE")
        void validPayloadReturnsCreated() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Ross 308");
            body.put("initialQty",  5000);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.flockId").isNotEmpty())
                    .andExpect(jsonPath("$.flockCode").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("EP-VEC-02 | Optional fields omitted (no supplierId, no notes) → 201 Created")
        void optionalFieldsOmitted() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Cobb 500");
            body.put("initialQty",  1000);
            body.put("arrivalDate", LocalDate.now().toString());

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        // ── EP: Invalid Equivalence Classes ──────────────────────────

        @Test
        @DisplayName("EP-IEC-01 | Missing breed → 400 Bad Request (Bean Validation @NotBlank) [FIX-3]")
        void missingBreedRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("initialQty",  1000);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-02 | Missing arrivalDate → 400 Bad Request (Bean Validation @NotNull) [FIX-3]")
        void missingArrivalDateRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",      "Ross 308");
            body.put("initialQty", 1000);

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-03 | initialQty = 0 → 400 Bad Request (Bean Validation @Min(1)) [FIX-3]")
        void zeroQuantityRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Ross 308");
            body.put("initialQty",  0);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-04 | Negative initialQty → 400 Bad Request (Bean Validation @Min(1)) [FIX-3]")
        void negativeQuantityRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Ross 308");
            body.put("initialQty",  -1);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-05 | Missing auth header → non-2xx (accessHelper rejects unauthenticated caller)")
        void missingAuthHeaderRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Ross 308");
            body.put("initialQty",  1000);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            // intentionally no AUTH_HEADER
                            .content(om.writeValueAsString(body)))
                    .andExpect(result ->
                            org.junit.jupiter.api.Assertions.assertTrue(
                                    result.getResponse().getStatus() >= 400,
                                    "Expected >=400 when auth header is absent"));
        }

        // ── BVA ──────────────────────────────────────────────────────

        @Test
        @DisplayName("BVA-01 | initialQty = 1 (lower boundary, just above 0) → 201 Created")
        void minQtyOneBoundary() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Test Breed");
            body.put("initialQty",  1);
            body.put("arrivalDate", "2025-06-01");

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("BVA-02 | initialQty = 100 000 (large upper value) → 201 Created")
        void largeQtyAccepted() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Large Batch");
            body.put("initialQty",  100_000);
            body.put("arrivalDate", "2025-06-01");

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        // ── EG ───────────────────────────────────────────────────────

        @Test
        @DisplayName("EG-01 | Empty string breed → 400 Bad Request (Bean Validation @NotBlank rejects \"\") [FIX-4]")
        void emptyBreedStringRejected() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "");
            body.put("initialQty",  500);
            body.put("arrivalDate", "2025-01-15");

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EG-02 | Future arrivalDate → 201 Created (no future-date restriction on arrival)")
        void futureArrivalDateAccepted() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Future Breed");
            body.put("initialQty",  100);
            body.put("arrivalDate", LocalDate.now().plusDays(30).toString());

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("EG-03 | Same breed registered twice → both succeed (no uniqueness constraint on breed)")
        void duplicateBreedAllowed() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("breed",       "Ross 308");
            body.put("initialQty",  1000);
            body.put("arrivalDate", "2025-01-01");

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated());

            mvc.perform(post("/api/flocks")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. PUT /api/flocks/{id} — Update Flock
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-002 Update Flock")
    class UpdateFlock {

        @Test
        @DisplayName("EP-VEC-03 | Update breed on ACTIVE flock → 200 OK, breed reflected in response")
        void updateActiveFlockBreed() throws Exception {
            String id = registerFlock("Old Breed", 1000, "2025-01-01");

            // currentQty (not initialQty) — controller maps req.getCurrentQty() to flock.currentQty
            Map<String, Object> update = new HashMap<>();
            update.put("breed",      "New Breed");
            update.put("currentQty", 950);

            mvc.perform(put("/api/flocks/" + id)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.breed").value("New Breed"));
        }

        @Test
        @DisplayName("EP-VEC-04 | Partial update (notes only) on ACTIVE flock → 200 OK, notes reflected")
        void partialUpdateNotesOnly() throws Exception {
            String id = registerFlock("Ross 308", 500, "2025-02-01");

            Map<String, Object> update = new HashMap<>();
            update.put("notes", "Updated batch reference");

            mvc.perform(put("/api/flocks/" + id)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Updated batch reference"));
        }

        @Test
        @DisplayName("EP-IEC-05 | Update CLOSED flock → 409 Conflict")
        void updateClosedFlockRejected() throws Exception {
            String id = registerFlock("Closed Breed", 500, "2024-01-01");

            // Close the flock (empty body → defaults to today)
            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            Map<String, Object> update = new HashMap<>();
            update.put("breed",      "Should Fail");
            update.put("currentQty", 100);

            mvc.perform(put("/api/flocks/" + id)
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(update)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("EG-04 | Update non-existent UUID → 404 Not Found")
        void updateNonExistentFlock() throws Exception {
            Map<String, Object> update = new HashMap<>();
            update.put("breed",      "Ghost");
            update.put("currentQty", 100);

            mvc.perform(put("/api/flocks/00000000-0000-0000-0000-000000000000")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(update)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  3. PATCH /api/flocks/{id}/close — Close Flock
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-003 Close Flock")
    class CloseFlock {

        @Test
        @DisplayName("EP-VEC-05 | Close ACTIVE flock with explicit closeDate → 200 OK, status CLOSED")
        void closeActiveFlockExplicitDate() throws Exception {
            String id = registerFlock("Finisher", 2000, "2025-01-01");

            Map<String, Object> body = new HashMap<>();
            body.put("closeDate", "2025-03-01");

            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"))
                    .andExpect(jsonPath("$.closeDate").value("2025-03-01"));
        }

        @Test
        @DisplayName("EP-VEC-06 | Close ACTIVE flock with no closeDate → 200 OK, closeDate defaults to today")
        void closeActiveFlockNoDateDefaultsToToday() throws Exception {
            String id = registerFlock("Auto Date", 500, "2024-01-01");

            // Empty body — controller falls back to LocalDate.now()
            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"))
                    .andExpect(jsonPath("$.closeDate").value(LocalDate.now().toString()));
        }

        @Test
        @DisplayName("EP-IEC-06 | Close already-CLOSED flock → 409 Conflict")
        void closeAlreadyClosedFlock() throws Exception {
            String id = registerFlock("Double Close", 300, "2025-01-01");

            Map<String, Object> body = new HashMap<>();
            body.put("closeDate", "2025-04-01");

            // First close — must succeed
            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isOk());

            // Second close attempt — must be rejected
            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("BVA-03 | closeDate = arrivalDate (same day close) → 200 OK (no before-arrival guard)")
        void closeDateEqualToArrivalDate() throws Exception {
            String id = registerFlock("Same Day", 100, "2025-05-01");

            Map<String, Object> body = new HashMap<>();
            body.put("closeDate", "2025-05-01");

            mvc.perform(patch("/api/flocks/" + id + "/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EG-05 | Close non-existent UUID → 404 Not Found")
        void closeNonExistentFlock() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("closeDate", "2025-04-01");

            mvc.perform(patch("/api/flocks/00000000-0000-0000-0000-000000000000/close")
                            .header(AUTH_HEADER, TEST_USER_ID)   // [FIX-1]
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  4. GET /api/flocks — List All Flocks  (no auth guard on GETs)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("List Flocks")
    class ListFlocks {

        @Test
        @DisplayName("EP-VEC-07 | GET /api/flocks → 200 OK, array body")
        void listFlocks() throws Exception {
            mvc.perform(get("/api/flocks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-08 | Registered flock appears in list")
        void registeredFlockAppearsInList() throws Exception {
            registerFlock("Listed Breed", 750, "2025-03-01");

            mvc.perform(get("/api/flocks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.breed == 'Listed Breed')]").exists());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  5. GET /api/flocks/{id}/audit — Audit History  (no auth guard)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Flock Audit History")
    class AuditHistory {

        @Test
        @DisplayName("EP-VEC-09 | Audit history for valid flock → 200 OK, array body")
        void auditForExistingFlock() throws Exception {
            String id = registerFlock("Audit Breed", 200, "2025-01-01");

            mvc.perform(get("/api/flocks/" + id + "/audit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-10 | Audit contains a FLOCK_CREATED entry after registration")
        void auditContainsCreatedEntry() throws Exception {
            String id = registerFlock("Audit Entry Breed", 300, "2025-01-01");

            mvc.perform(get("/api/flocks/" + id + "/audit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.action == 'FLOCK_CREATED')]").exists());
        }

        @Test
        @DisplayName("EG-06 | Audit for unknown UUID → 200 OK with empty array (no 404 guard in controller)")
        void auditForUnknownFlockReturnsEmptyList() throws Exception {
            // auditLogRepository.findByFlockIdOrderByLoggedAtDesc() returns []
            // for any unknown ID; the controller wraps it in 200 directly.
            mvc.perform(get("/api/flocks/00000000-0000-0000-0000-000000000000/audit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
