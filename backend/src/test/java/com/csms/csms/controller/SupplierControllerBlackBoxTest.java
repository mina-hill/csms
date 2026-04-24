package com.csms.csms.controller;

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

import java.util.HashMap;
import java.util.Map;

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
 * Design notes (derived from actual SupplierController source):
 *
 *   1. supplierType is a Java enum (SupplierType). Valid values are:
 *      FEED, MEDICINE, BRADA, CHICKS, EQUIPMENT, OTHER.
 *      Sending an unrecognised string causes Jackson deserialization to
 *      fail, which Spring returns as 400 Bad Request.
 *
 *   2. Duplicate-name check uses supplierRepository.existsByName()
 *      which maps to a SQL exact-match (case-sensitive in PostgreSQL by
 *      default). The case-insensitive duplicate test (EG-02 from the
 *      original file) is therefore removed — it would not conflict
 *      unless the DB collation is case-insensitive.
 *
 *   3. createSupplier() returns ResponseEntity.badRequest().build()
 *      (no body) for a missing/blank name. Status is 400 but the
 *      response body is empty.
 *
 *   4. DELETE is a soft-delete: it sets isActive = false and returns
 *      204 No Content. The deleted supplier still exists in the DB.
 *
 *   5. GET /api/suppliers/search/name performs an EXACT name match
 *      and returns 404 when nothing is found.
 *
 *   6. BVA on name length is not enforced by the controller; the DB
 *      schema uses VARCHAR with no explicit length cap shown in the
 *      DDL, so BVA-02/03 (100/101-char name) tests are removed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Supplier Management — Black-Box Tests")
class SupplierControllerBlackBoxTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    /** Creates a supplier and returns its supplierId UUID string. */
    private String createSupplier(String name, String type) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name",         name);
        b.put("supplierType", type);

        String resp = mvc.perform(post("/api/suppliers")
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.supplierId").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Golden Grain Co"));
        }

        @Test
        @DisplayName("EP-VEC-02 | All valid supplierType enum values accepted → 201 each")
        void allValidSupplierTypesAccepted() throws Exception {
            // SupplierType enum values as declared in the Java source
            String[] types = {"FEED", "MEDICINE", "BRADA", "CHICKS", "EQUIPMENT", "OTHER"};
            for (int i = 0; i < types.length; i++) {
                Map<String, Object> b = new HashMap<>();
                b.put("name",         "Type Test Supplier " + i);
                b.put("supplierType", types[i]);

                mvc.perform(post("/api/suppliers")
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
            b.put("name",         "Unique Farms");   // exact duplicate
            b.put("supplierType", "MEDICINE");

            mvc.perform(post("/api/suppliers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("EG-02 | Empty string name → 400 Bad Request")
        void emptyStringNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "");
            b.put("supplierType", "FEED");

            mvc.perform(post("/api/suppliers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EG-03 | Whitespace-only name → 400 Bad Request")
        void whitespaceOnlyNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "   ");
            b.put("supplierType", "FEED");

            mvc.perform(post("/api/suppliers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        // ── BVA: Name Length ──────────────────────────────────────────

        @Test
        @DisplayName("BVA-01 | name = 1 character (lower boundary) → 201 Created")
        void singleCharNameAccepted() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Z");
            b.put("supplierType", "OTHER");

            mvc.perform(post("/api/suppliers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. Read Operations
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Read Suppliers")
    class ReadSuppliers {

        @Test
        @DisplayName("EP-VEC-04 | GET /api/suppliers → 200 OK, array body")
        void listAllSuppliers() throws Exception {
            mvc.perform(get("/api/suppliers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-05 | GET /api/suppliers/active → 200 OK, array of active suppliers")
        void listActiveSuppliers() throws Exception {
            mvc.perform(get("/api/suppliers/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-06 | GET /api/suppliers/{id} for known supplier → 200 OK, correct name")
        void getKnownSupplier() throws Exception {
            String id = createSupplier("Known Supplier Ltd", "MEDICINE");

            mvc.perform(get("/api/suppliers/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Known Supplier Ltd"))
                    .andExpect(jsonPath("$.supplierId").value(id));
        }

        @Test
        @DisplayName("EG-04 | GET /api/suppliers/{id} unknown UUID → 404 Not Found")
        void getUnknownSupplier() throws Exception {
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(b)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("BVA-02 | Rename supplier to its own current name → 200 OK (no self-conflict)")
        void renameToSameNameAllowed() throws Exception {
            String id = createSupplier("Same Name Co", "FEED");

            // Controller only checks for conflict if the new name != existing name.
            // Setting the same name bypasses the duplicate check.
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "Same Name Co");
            b.put("supplierType", "FEED");

            mvc.perform(put("/api/suppliers/" + id)
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

            mvc.perform(delete("/api/suppliers/" + id))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("EG-09 | DELETE unknown UUID → 404 Not Found")
        void deleteUnknownSupplier() throws Exception {
            mvc.perform(delete("/api/suppliers/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EG-10 | Soft-deleted supplier no longer appears in /api/suppliers/active list")
        void softDeletedSupplierNotInActiveList() throws Exception {
            String id = createSupplier("Soon Gone Ltd", "FEED");

            mvc.perform(delete("/api/suppliers/" + id))
                    .andExpect(status().isNoContent());

            // Supplier is deactivated — must not appear in the active list
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

            mvc.perform(delete("/api/suppliers/" + id))
                    .andExpect(status().isNoContent());

            // Record still exists — GET by ID returns 200 with isActive = false
            mvc.perform(get("/api/suppliers/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));
        }
    }
}
