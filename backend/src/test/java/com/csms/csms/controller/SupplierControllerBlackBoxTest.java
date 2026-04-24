package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.User;
import com.csms.csms.entity.UserRole;
import com.csms.csms.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Black-Box Test Suite — Supplier Management Module
 *
 * Techniques applied:
 *   EP  = Equivalence Partitioning
 *   BVA = Boundary Value Analysis
 *   EG  = Error Guessing
 *
 * Endpoints under test:
 *   GET    /api/suppliers                     – list all
 *   GET    /api/suppliers/active              – list active only
 *   GET    /api/suppliers/type/{type}         – filter by type
 *   GET    /api/suppliers/{id}                – fetch one
 *   GET    /api/suppliers/search/name?name=   – search by exact name
 *   POST   /api/suppliers                     – create
 *   PUT    /api/suppliers/{id}                – update
 *   DELETE /api/suppliers/{id}                – soft-delete (marks isActive = false)
 *
 * KEY FIXES from original:
 *   1. Added test user creation in @BeforeEach
 *   2. All POST/PUT/DELETE include X-CSMS-User-Id header (required by CsmsAccessHelper)
 *   3. Helper method now passes required auth header
 *   4. Suppressed mockUser requirement; using real User entity instead
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Supplier Management — Black-Box Tests")
class SupplierControllerBlackBoxTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired com.csms.csms.repository.SupplierRepository supplierRepository;
    @Autowired com.csms.csms.repository.MedicinePurchaseRepository medicinePurchaseRepository;
    @Autowired com.csms.csms.repository.FeedPurchaseRepository feedPurchaseRepository;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String actorId; // UUID string of test user with ACCOUNTANT role

    // ─────────────────────────────────────────────
    //  Setup
    // ─────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Clear previous test data to ensure isolation
        // Use native SQL to clear linked tables due to complex dependencies
        jdbcTemplate.execute("TRUNCATE TABLE medicine_purchases CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE feed_purchases CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE suppliers CASCADE");
        userRepository.deleteAll();
        
        // Create a test user with ACCOUNTANT role (satisfies requireFinancialOrThrow)
        User testUser = new User();
        testUser.setUsername("test_actor_" + UUID.randomUUID());
        testUser.setEmail("test_" + UUID.randomUUID() + "@test.com");
        testUser.setRole(UserRole.ACCOUNTANT);
        testUser.setIsActive(true);
        User saved = userRepository.save(testUser);
        actorId = saved.getUserId().toString();
    }

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    /**
     * Creates a supplier and returns its supplierId UUID string.
     * Now includes the required X-CSMS-User-Id header.
     */
    private String createSupplier(String name, String type) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name",         name);
        b.put("supplierType", type);

        String resp = mvc.perform(post("/api/suppliers")
                .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(b)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return om.readTree(resp).get("supplierId").asText();
    }

    // ─────────────────────────────────────────────────────────────────
    //  1. POST /api/suppliers — Create Supplier
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-007 Create Supplier")
    class CreateSupplier {

        // ── EP: Valid Equivalence Classes ────────────────────────────

        @Test
        @DisplayName("EP-VEC-01 | FEED supplier with name and type → 201, supplierId returned")
        void validFeedSupplierCreated() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Golden Grain Co");
            b.put("supplierType", "FEED");
            b.put("phone",        "+92-300-1234567");
            b.put("address",      "Lahore");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.supplierId").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Golden Grain Co"));
        }

        @Test
        @DisplayName("EP-VEC-02 | All valid supplierType enum values accepted → 201 each")
        void allValidSupplierTypesAccepted() throws Exception {
            String[] types = {"FEED", "MEDICINE", "BRADA", "CHICKS", "EQUIPMENT", "OTHER"};
            for (int i = 0; i < types.length; i++) {
                Map<String, Object> b = new HashMap<>();
                b.put("name",         "Type Test Supplier " + i);
                b.put("supplierType", types[i]);

                mvc.perform(post("/api/suppliers")
                        .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                        .andExpect(status().isCreated());
            }
        }

        @Test
        @DisplayName("EP-VEC-03 | Optional fields omitted (no phone, no address) → 201 Created")
        void optionalFieldsOmitted() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Minimal Supplier");
            b.put("supplierType", "OTHER");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        // ── EP: Invalid Equivalence Classes ──────────────────────────

        @Test
        @DisplayName("EP-IEC-01 | Missing name → 400 Bad Request")
        void missingNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("supplierType", "FEED");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-02 | Invalid supplierType string → 400 (Jackson enum deserialization fails)")
        void invalidSupplierTypeRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Bad Type Supplier");
            b.put("supplierType", "INVALID_TYPE");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        // ── EG: Duplicate / Edge-case Names ──────────────────────────

        @Test
        @DisplayName("EG-01 | Exact duplicate name → 409 Conflict")
        void duplicateExactNameRejected() throws Exception {
            createSupplier("Unique Farms", "FEED");

            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Unique Farms");
            b.put("supplierType", "MEDICINE");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("EG-02 | Blank name (whitespace only) → 400 Bad Request")
        void blankNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "   ");
            b.put("supplierType", "OTHER");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EG-03 | Missing auth header → 401 Unauthorized")
        void missingAuthHeaderRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Auth Test Supplier");
            b.put("supplierType", "FEED");

            mvc.perform(post("/api/suppliers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("EG-04 | Invalid UUID in auth header → 401 Unauthorized")
        void invalidAuthUuidRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Invalid Auth Supplier");
            b.put("supplierType", "FEED");

            mvc.perform(post("/api/suppliers")
                    .header(CsmsAccessHelper.USER_ID_HEADER, "not-a-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. GET /api/suppliers — List Suppliers
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-007 List & Retrieve Suppliers")
    class ListSuppliers {

        @Test//test not passing 
        @DisplayName("EP-VEC-04 | GET /api/suppliers empty list → 200 OK, empty array")
        void listSuppliersEmpty() throws Exception {
            mvc.perform(get("/api/suppliers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));//5!=0
        }

        @Test
        @DisplayName("EP-VEC-05 | GET /api/suppliers with data → 200 OK, array with entries")
        void listSuppliersWithData() throws Exception {
            createSupplier("List Test Co", "FEED");

            mvc.perform(get("/api/suppliers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == 'List Test Co')]").exists());
        }

        @Test
        @DisplayName("EP-VEC-06 | GET /api/suppliers/active filters to isActive=true only")
        void listActiveSuppliers() throws Exception {
            String id = createSupplier("Active Supplier Ltd", "MEDICINE");

            mvc.perform(get("/api/suppliers/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.name == 'Active Supplier Ltd')]").exists());
        }

        @Test
        @DisplayName("EG-04 | GET /api/suppliers/{id} non-existent UUID → 404 Not Found")
        void getSupplierByIdNotFound() throws Exception {
            mvc.perform(get("/api/suppliers/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-VEC-07 | GET /api/suppliers/search/name exact match → 200 OK")
        void searchByExactName() throws Exception {
            createSupplier("SearchTarget Farms", "CHICKS");

            mvc.perform(get("/api/suppliers/search/name")
                    .param("name", "SearchTarget Farms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("SearchTarget Farms"));
        }

        @Test
        @DisplayName("EG-05 | GET /api/suppliers/search/name no match → 404 Not Found")
        void searchByNameNoMatch() throws Exception {
            mvc.perform(get("/api/suppliers/search/name")
                    .param("name", "NonExistentSupplierXYZ"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-VEC-08 | GET /api/suppliers/type/FEED → 200 OK, array body")
        void filterByTypeFeed() throws Exception {
            createSupplier("Feed Filter Co", "FEED");

            mvc.perform(get("/api/suppliers/type/FEED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == 'Feed Filter Co')]").exists());
        }

        @Test
        @DisplayName("EG-06 | GET /api/suppliers/type/INVALID_TYPE → 400 Bad Request")
        void filterByInvalidType() throws Exception {
            mvc.perform(get("/api/suppliers/type/INVALID_TYPE"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  3. PUT /api/suppliers/{id} — Update Supplier
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-007 Update Supplier")
    class UpdateSupplier {

        @Test
        @DisplayName("EP-VEC-09 | Update name of existing supplier → 200 OK, new name reflected")
        void updateSupplierName() throws Exception {
            String id = createSupplier("Old Name Co", "FEED");

            Map<String, Object> b = new HashMap<>();
            b.put("name",         "New Name Co");
            b.put("supplierType", "FEED");

            mvc.perform(put("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name Co"));
        }

        @Test
        @DisplayName("EP-VEC-10 | Update type of existing supplier → 200 OK, new type reflected")
        void updateSupplierType() throws Exception {
            String id = createSupplier("Type Change Co", "FEED");

            Map<String, Object> b = new HashMap<>();
            b.put("supplierType", "MEDICINE");

            mvc.perform(put("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.supplierType").value("MEDICINE"));
        }

        @Test
        @DisplayName("EG-07 | Rename to a name already taken by another supplier → 409 Conflict")
        void renameToExistingNameRejected() throws Exception {
            createSupplier("Taken Name Ltd", "FEED");
            String id = createSupplier("My Supplier Co", "MEDICINE");

            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Taken Name Ltd");
            b.put("supplierType", "MEDICINE");

            mvc.perform(put("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("EG-08 | Update non-existent UUID → 404 Not Found")
        void updateUnknownSupplier() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Ghost Update");
            b.put("supplierType", "OTHER");

            mvc.perform(put("/api/suppliers/00000000-0000-0000-0000-000000000000")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("BVA-02 | Rename supplier to its own current name → 200 OK (no self-conflict)")
        void renameToSameNameAllowed() throws Exception {
            String id = createSupplier("Same Name Co", "FEED");

            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Same Name Co");
            b.put("supplierType", "FEED");

            mvc.perform(put("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  4. DELETE /api/suppliers/{id} — Soft-Delete Supplier
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Delete (Soft-Delete) Supplier")
    class DeleteSupplier {

        @Test
        @DisplayName("EP-VEC-11 | DELETE known supplier → 204 No Content")
        void deleteKnownSupplier() throws Exception {
            String id = createSupplier("To Be Deleted Co", "OTHER");

            mvc.perform(delete("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("EG-09 | DELETE unknown UUID → 404 Not Found")
        void deleteUnknownSupplier() throws Exception {
            mvc.perform(delete("/api/suppliers/00000000-0000-0000-0000-000000000000")
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EG-10 | Soft-deleted supplier no longer appears in /api/suppliers/active list")
        void softDeletedSupplierNotInActiveList() throws Exception {
            String id = createSupplier("Soon Gone Ltd", "FEED");

            mvc.perform(delete("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/suppliers/active"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString();
                        org.junit.jupiter.api.Assertions.assertFalse(
                                body.contains("Soon Gone Ltd"),
                                "Soft-deleted supplier should not appear in active list");
                    });
        }

        @Test
        @DisplayName("EG-11 | Soft-deleted supplier still retrievable by ID (record not physically removed)")
        void softDeletedSupplierStillExistsById() throws Exception {
            String id = createSupplier("Archived Supplier Co", "MEDICINE");

            mvc.perform(delete("/api/suppliers/" + id)
                    .header(CsmsAccessHelper.USER_ID_HEADER, actorId))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/suppliers/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));
        }
    }
}