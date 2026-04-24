//package com.csms.csms.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import com.csms.csms.repository.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * Black-Box Test Suite — Feed Management Module
// *
// * Techniques applied:
// *   EP  = Equivalence Partitioning
// *   BVA = Boundary Value Analysis
// *   EG  = Error Guessing
// *
// * Endpoints under test:
// *   POST /api/feed/purchases     – stock in          (US-012)
// *   POST /api/feed/usage         – stock out          (US-013)
// *   POST /api/feed/sales         – surplus sack sale  (US-014)
// *   POST /api/feed-types/upsert  – create/update feed type
// *   GET  /api/feed-types         – list all types / stock report (US-015)
// *
// * FIX: Removed @Transactional from the class.
// *      The DB stock counter is updated by a trigger that runs in its own
// *      transaction. With @Transactional on the test, the trigger commits its
// *      sub-transaction but the JPA entity read inside the same rolled-back test
// *      transaction still sees the old (pre-trigger) value → currentStock = 0.
// *      Removing @Transactional lets every operation commit normally so stock
// *      reads reflect real state.
// *
// *      @AfterEach deletes ONLY the rows created by this test run, identified
// *      by the IDs collected in createdPurchaseIds / createdUsageIds /
// *      createdSaleIds, plus the setUp() supplier, feedType, and flock.
// *      Pre-existing production data is never touched.
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//// @Transactional intentionally removed — see class javadoc above
//@DisplayName("Feed Management — Black-Box Tests")
//class FeedControllerBlackBoxTest {
//
//    @Autowired MockMvc mvc;
//    @Autowired ObjectMapper om;
//
//    // Repositories injected solely for @AfterEach cleanup
//    @Autowired FeedUsageRepository    feedUsageRepository;
//    @Autowired FeedSaleRepository     feedSaleRepository;
//    @Autowired FeedPurchaseRepository feedPurchaseRepository;
//    @Autowired FeedTypeRepository     feedTypeRepository;
//    @Autowired FlockRepository        flockRepository;
//    @Autowired SupplierRepository     supplierRepository;
//
//    private String supplierId;
//    private String feedTypeId;
//    private String flockId;
//
//    // Tracks IDs created during each test so tearDown deletes only those rows
//    private final List<UUID> createdPurchaseIds = new ArrayList<>();
//    private final List<UUID> createdUsageIds    = new ArrayList<>();
//    private final List<UUID> createdSaleIds     = new ArrayList<>();
//
//    // ─────────────────────────────────────────────
//    //  Setup / Teardown
//    // ─────────────────────────────────────────────
//
//    @BeforeEach
//    void setUp() throws Exception {
//        supplierId = createSupplier("Feed Supplier Co " + UUID.randomUUID());
//        feedTypeId = upsertFeedType("Starter_" + UUID.randomUUID(), 5);
//        flockId    = createFlock("Ross 308", 5000, "2025-01-01");
//        createdPurchaseIds.clear();
//        createdUsageIds.clear();
//        createdSaleIds.clear();
//    }
//
//    /**
//     * Deletes only the rows this test created, in FK-safe order.
//     * Pre-existing production data is never touched.
//     */
//    @AfterEach
//    void tearDown() {
//        createdUsageIds.forEach(feedUsageRepository::deleteById);
//        createdSaleIds.forEach(feedSaleRepository::deleteById);
//        createdPurchaseIds.forEach(feedPurchaseRepository::deleteById);
//        feedTypeRepository.deleteById(UUID.fromString(feedTypeId));
//        flockRepository.deleteById(UUID.fromString(flockId));
//        supplierRepository.deleteById(UUID.fromString(supplierId));
//    }
//
//    // ─── helpers ──────────────────────────────────────────────────────
//
//    private String createSupplier(String name) throws Exception {
//        Map<String, Object> b = new HashMap<>();
//        b.put("name", name);
//        b.put("supplierType", "FEED");
//        String resp = mvc.perform(post("/api/suppliers")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(b)))
//                .andExpect(status().isCreated())
//                .andReturn().getResponse().getContentAsString();
//        return om.readTree(resp).get("supplierId").asText();
//    }
//
//    private String upsertFeedType(String name, int threshold) throws Exception {
//        Map<String, Object> b = new HashMap<>();
//        b.put("name",         name);
//        b.put("minThreshold", threshold);
//        String resp = mvc.perform(post("/api/feed-types/upsert")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(b)))
//                .andExpect(result -> {
//                    int s = result.getResponse().getStatus();
//                    Assertions.assertTrue(s == 200 || s == 201,
//                            "upsert expected 200 or 201, got: " + s);
//                })
//                .andReturn().getResponse().getContentAsString();
//        return om.readTree(resp).get("feedTypeId").asText();
//    }
//
//    private String createFlock(String breed, int qty, String date) throws Exception {
//        Map<String, Object> b = new HashMap<>();
//        b.put("breed",       breed);
//        b.put("initialQty",  qty);
//        b.put("arrivalDate", date);
//        String resp = mvc.perform(post("/api/flocks")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(b)))
//                .andExpect(status().isCreated())
//                .andReturn().getResponse().getContentAsString();
//        return om.readTree(resp).get("flockId").asText();
//    }
//
//    private void purchaseSacks(int count) throws Exception {
//        Map<String, Object> b = new HashMap<>();
//        b.put("feedTypeId",   feedTypeId);
//        b.put("supplierId",   supplierId);
//        b.put("sacksQty",     count);
//        b.put("costPerSack",  250);
//        b.put("purchaseDate", "2025-01-10");
//        String resp = mvc.perform(post("/api/feed/purchases")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(b)))
//                .andExpect(status().isCreated())
//                .andReturn().getResponse().getContentAsString();
//        createdPurchaseIds.add(UUID.fromString(om.readTree(resp).get("purchaseId").asText()));
//    }
//
//    /** Records usage and tracks the ID for teardown. Returns the response status. */
//    private void recordUsage(Map<String, Object> body, int expectedStatus) throws Exception {
//        String resp = mvc.perform(post("/api/feed/usage")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(body)))
//                .andExpect(status().is(expectedStatus))
//                .andReturn().getResponse().getContentAsString();
//        if ((expectedStatus == 200 || expectedStatus == 201) && !resp.isBlank()) {
//            com.fasterxml.jackson.databind.JsonNode n = om.readTree(resp);
//            if (n.has("usageId")) createdUsageIds.add(UUID.fromString(n.get("usageId").asText()));
//        }
//    }
//
//    /** Records a feed sale and tracks the ID for teardown. */
//    private void recordSale(Map<String, Object> body, int expectedStatus) throws Exception {
//        String resp = mvc.perform(post("/api/feed/sales")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(body)))
//                .andExpect(status().is(expectedStatus))
//                .andReturn().getResponse().getContentAsString();
//        if (expectedStatus == 201 && !resp.isBlank()) {
//            com.fasterxml.jackson.databind.JsonNode n = om.readTree(resp);
//            if (n.has("saleId")) createdSaleIds.add(UUID.fromString(n.get("saleId").asText()));
//        }
//    }
//
//    /** Returns currentStock for the feedTypeId created in setUp(). */
//    private int getStockForCurrentFeedType() throws Exception {
//        String resp = mvc.perform(get("/api/feed-types"))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//        for (com.fasterxml.jackson.databind.JsonNode node : om.readTree(resp)) {
//            if (feedTypeId.equals(node.get("feedTypeId").asText())) {
//                return node.get("currentStock").asInt();
//            }
//        }
//        return -1;
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  1. Feed Purchases  (US-012)
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-012 Feed Purchase")
//    class FeedPurchase {
//
//        @Test
//        @DisplayName("EP-VEC-01 | Valid purchase → 201 Created")
//        void validPurchaseReturnsCreated() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     50);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @DisplayName("EP-VEC-02 | Purchase increments feed type currentStock")
//        void purchaseIncreasesStock() throws Exception {
//            int oldStock = getStockForCurrentFeedType();
//            purchaseSacks(40);
//            int stock = getStockForCurrentFeedType();
//            Assertions.assertEquals(oldStock + 40, stock,
//                    "currentStock should equal purchased quantity added to old stock");
//        }
//
//        @Test
//        @DisplayName("EP-IEC-01 | sacksQty = 0 → 400 Bad Request")
//        void zeroSackQtyRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     0);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("EP-IEC-02 | Negative sacksQty → 400 Bad Request")
//        void negativeSackQtyRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     -10);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("EP-IEC-03 | costPerSack = 0 → 400 Bad Request")
//        void zeroCostRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     10);
//            b.put("costPerSack",  0);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("EP-IEC-04 | Missing supplierId → 400 Bad Request")
//        void missingSupplierRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("sacksQty",     20);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("EP-IEC-05 | Missing feedTypeId → 400 Bad Request")
//        void missingFeedTypeRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     20);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("BVA-01 | sacksQty = 1 (lower boundary) → 201 Created")
//        void minOneSackBoundary() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     1);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @DisplayName("BVA-02 | costPerSack = 1 (minimum positive) → 201 Created")
//        void minCostPerSackBoundary() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   feedTypeId);
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     10);
//            b.put("costPerSack",  1);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @DisplayName("EG-01 | Unknown feedTypeId (random UUID) → 400 Bad Request")
//        void unknownFeedTypeRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("feedTypeId",   UUID.randomUUID().toString());
//            b.put("supplierId",   supplierId);
//            b.put("sacksQty",     10);
//            b.put("costPerSack",  300);
//            b.put("purchaseDate", "2025-02-01");
//
//            mvc.perform(post("/api/feed/purchases")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  2. Feed Usage  (US-013)
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-013 Feed Usage")
//    class FeedUsage {
//
//        @Test
//        @DisplayName("EP-VEC-03 | Valid DAY usage within stock → 201 Created")
//        void validDayUsageCreated() throws Exception {
//            purchaseSacks(100);
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  20);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "DAY");
//
//            recordUsage(b, 201);
//        }
//
//        @Test
//        @DisplayName("EP-VEC-04 | Valid NIGHT usage within stock → 201 Created")
//        void validNightUsageCreated() throws Exception {
//            purchaseSacks(100);
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  10);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "NIGHT");
//
//            recordUsage(b, 201);
//        }
//
//        @Test
//        @DisplayName("EP-IEC-06 | shift absent → 400 Bad Request")
//        void missingShiftRejected() throws Exception {
//            purchaseSacks(100);
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  10);
//            b.put("recordDate", "2025-01-20");
//
//            recordUsage(b, 400);
//        }
//
//        @Test
//        @DisplayName("EP-IEC-07 | Invalid shift value → 400 Bad Request")
//        void invalidShiftRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  5);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "AFTERNOON");
//
//            recordUsage(b, 400);
//        }
//
//        @Test
//        @DisplayName("EP-IEC-08 | sacksUsed = 0 → 400 Bad Request")
//        void zeroSacksUsedRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  0);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "DAY");
//
//            recordUsage(b, 400);
//        }
//
//        @Test
//        @DisplayName("BVA-03 | sacksUsed exactly equals stock → 201 (exact depletion), then stock = 0")
//        void usageExactlyEqualsStock() throws Exception {
//            purchaseSacks(10);
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  10);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "DAY");
//
//            recordUsage(b, 201);
//
//            int stock = getStockForCurrentFeedType();
//            Assertions.assertEquals(0, stock, "Stock must be 0 after exact depletion");
//        }
//
//        @Test
//        @DisplayName("EG-02 | Duplicate (same flock+feedType+date+shift) → 200 OK (upsert)")
//        void duplicateUsageSameDayUpserts() throws Exception {
//            purchaseSacks(100);
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("flockId",    flockId);
//            b.put("feedTypeId", feedTypeId);
//            b.put("sacksUsed",  5);
//            b.put("recordDate", "2025-01-20");
//            b.put("shift",      "DAY");
//
//            recordUsage(b, 201);
//
//            recordUsage(b, 200);
//        }
//
//        @Test
//        @DisplayName("EG-03 | DAY and NIGHT entries same flock+feedType+date → both 201 (different shift key)")
//        void dayAndNightEntriesCoexist() throws Exception {
//            purchaseSacks(100);
//
//            Map<String, Object> day = new HashMap<>();
//            day.put("flockId",    flockId);
//            day.put("feedTypeId", feedTypeId);
//            day.put("sacksUsed",  5);
//            day.put("recordDate", "2025-01-21");
//            day.put("shift",      "DAY");
//
//            Map<String, Object> night = new HashMap<>();
//            night.put("flockId",    flockId);
//            night.put("feedTypeId", feedTypeId);
//            night.put("sacksUsed",  3);
//            night.put("recordDate", "2025-01-21");
//            night.put("shift",      "NIGHT");
//
//            recordUsage(day, 201);
//
//            recordUsage(night, 201);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  3. Feed Sales  (US-014)
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-014 Feed Sack Sales")
//    class FeedSale {
//
//        @Test
//        @DisplayName("EP-VEC-05 | Valid sale → 201 Created")
//        void validFeedSaleCreated() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("saleDate",     "2025-03-01");
//            b.put("sacksQty",     10);
//            b.put("pricePerSack", 350);
//            b.put("buyerName",    "Ahmed Traders");
//
//            recordSale(b, 201);
//        }
//
//        @Test
//        @DisplayName("BVA-04 | sacksQty = 1 (minimum) → 201 Created")
//        void minOneSackSale() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("saleDate",     "2025-03-01");
//            b.put("sacksQty",     1);
//            b.put("pricePerSack", 350);
//            b.put("buyerName",    "Single Sack Buyer");
//
//            recordSale(b, 201);
//        }
//
//        @Test
//        @DisplayName("GET /api/feed/sales → 200 OK, array body")
//        void listFeedSales() throws Exception {
//            mvc.perform(get("/api/feed/sales"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray());
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    //  4. Feed Stock Report  (US-015)
//    // ─────────────────────────────────────────────────────────────────
//    @Nested
//    @DisplayName("US-015 Feed Stock Report")
//    class FeedStockReport {
//
//        @Test
//        @DisplayName("EP-VEC-06 | GET /api/feed-types → 200 OK, array body")
//        void getFeedTypes() throws Exception {
//            mvc.perform(get("/api/feed-types"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray());
//        }
//
//        @Test
//        @DisplayName("EP-VEC-07 | Purchased feed type shows correct currentStock")
//        void purchasedFeedTypeAppearsInStockList() throws Exception {
//            int oldStock = getStockForCurrentFeedType();
//            purchaseSacks(30);
//            int stock = getStockForCurrentFeedType();
//            Assertions.assertEquals(30 + oldStock, stock,
//                    "currentStock must equal number of sacks purchased added to old stock");
//        }
//
//        @Test
//        @DisplayName("EP-VEC-08 | GET /api/feed-types/low-stock → 200 OK, array of low-stock types")
//        void getLowStockFeedTypes() throws Exception {
//            mvc.perform(get("/api/feed-types/low-stock"))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$").isArray());
//        }
//
//        @Test
//        @DisplayName("EP-VEC-09 | POST /api/feed-types/upsert existing name → 200 OK (update threshold)")
//        void upsertExistingFeedTypeUpdatesThreshold() throws Exception {
//            String resp = mvc.perform(get("/api/feed-types"))
//                    .andExpect(status().isOk())
//                    .andReturn().getResponse().getContentAsString();
//            String typeName = null;
//            for (com.fasterxml.jackson.databind.JsonNode node : om.readTree(resp)) {
//                if (feedTypeId.equals(node.get("feedTypeId").asText())) {
//                    typeName = node.get("name").asText();
//                    break;
//                }
//            }
//            Assertions.assertNotNull(typeName, "Feed type must be found in list");
//
//            Map<String, Object> b = new HashMap<>();
//            b.put("name",         typeName);
//            b.put("minThreshold", 10);
//
//            mvc.perform(post("/api/feed-types/upsert")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.minThreshold").value(10));
//        }
//
//        @Test
//        @DisplayName("EP-IEC-09 | POST /api/feed-types/upsert with blank name → 400 Bad Request")
//        void blankFeedTypeNameRejected() throws Exception {
//            Map<String, Object> b = new HashMap<>();
//            b.put("name",         "");
//            b.put("minThreshold", 5);
//
//            mvc.perform(post("/api/feed-types/upsert")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(om.writeValueAsString(b)))
//                    .andExpect(status().isBadRequest());
//        }
//    }
//}

package com.csms.csms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Black-Box Test Suite — Feed Management Module
 *
 * Techniques applied:
 *   EP  = Equivalence Partitioning
 *   BVA = Boundary Value Analysis
 *   EG  = Error Guessing
 *
 * Endpoints under test:
 *   POST /api/feed/purchases     – stock in          (US-012)
 *   POST /api/feed/usage         – stock out          (US-013)
 *   POST /api/feed/sales         – surplus sack sale  (US-014)
 *   POST /api/feed-types/upsert  – create/update feed type
 *   GET  /api/feed-types         – list all types / stock report (US-015)
 *
 * FIX: Removed @Transactional from the class.
 *      The DB stock counter is updated by a trigger that runs in its own
 *      transaction. With @Transactional on the test, the trigger commits its
 *      sub-transaction but the JPA entity read inside the same rolled-back test
 *      transaction still sees the old (pre-trigger) value → currentStock = 0.
 *      Removing @Transactional lets every operation commit normally so stock
 *      reads reflect real state.
 *
 *      @AfterEach deletes ONLY the rows created by this test run, identified
 *      by feedTypeId / flockId / supplierId via JdbcTemplate native deletes.
 *      Pre-existing production data is never touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
// @Transactional intentionally removed — see class javadoc above
@DisplayName("Feed Management — Black-Box Tests")
class FeedControllerBlackBoxTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Autowired JdbcTemplate jdbc;

    private String supplierId;
    private String feedTypeId;
    private String flockId;

    // ─────────────────────────────────────────────
    //  Setup / Teardown
    // ─────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        supplierId = createSupplier("Feed Supplier Co " + UUID.randomUUID());
        feedTypeId = upsertFeedType("Starter_" + UUID.randomUUID(), 5);
        flockId    = createFlock("Ross 308", 5000, "2025-01-01");

    }

    /**
     * Deletes only rows tied to this test's feedTypeId / flockId / supplierId.
     * Uses JdbcTemplate so we can issue native deletes in FK-safe order without
     * needing custom repository methods or touching any other data.
     */
    @AfterEach
    void tearDown() {
        // feed_usage and feed_purchases reference both flock and feed_type
        jdbc.update("DELETE FROM feed_usage    WHERE flock_id    = ?::uuid", flockId);
        jdbc.update("DELETE FROM feed_usage    WHERE feed_type_id = ?::uuid", feedTypeId);
        jdbc.update("DELETE FROM feed_purchases WHERE feed_type_id = ?::uuid", feedTypeId);
        // feed_sales has no flock/feed_type FK in your schema — delete by supplierId is not applicable
        // flock and feed_type can now be safely removed
        jdbc.update("DELETE FROM flocks      WHERE flock_id    = ?::uuid", flockId);
        jdbc.update("DELETE FROM feed_types  WHERE feed_type_id = ?::uuid", feedTypeId);
        jdbc.update("DELETE FROM suppliers   WHERE supplier_id  = ?::uuid", supplierId);
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private String createSupplier(String name) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name", name);
        b.put("supplierType", "FEED");
        String resp = mvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("supplierId").asText();
    }

    private String upsertFeedType(String name, int threshold) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name",         name);
        b.put("minThreshold", threshold);
        String resp = mvc.perform(post("/api/feed-types/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assertions.assertTrue(s == 200 || s == 201,
                            "upsert expected 200 or 201, got: " + s);
                })
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("feedTypeId").asText();
    }

    private String createFlock(String breed, int qty, String date) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("breed",       breed);
        b.put("initialQty",  qty);
        b.put("arrivalDate", date);
        String resp = mvc.perform(post("/api/flocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("flockId").asText();
    }

    private void purchaseSacks(int count) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("feedTypeId",   feedTypeId);
        b.put("supplierId",   supplierId);
        b.put("sacksQty",     count);
        b.put("costPerSack",  250);
        b.put("purchaseDate", "2025-01-10");
        mvc.perform(post("/api/feed/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                .andExpect(status().isCreated());
    }

    private void recordUsage(Map<String, Object> body, int expectedStatus) throws Exception {
        mvc.perform(post("/api/feed/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus));
    }

    private void recordSale(Map<String, Object> body, int expectedStatus) throws Exception {
        mvc.perform(post("/api/feed/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus));
    }

    /** Returns currentStock for the feedTypeId created in setUp(). */
    private int getStockForCurrentFeedType() throws Exception {
        String resp = mvc.perform(get("/api/feed-types"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (com.fasterxml.jackson.databind.JsonNode node : om.readTree(resp)) {
            if (feedTypeId.equals(node.get("feedTypeId").asText())) {
                return node.get("currentStock").asInt();
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────
    //  1. Feed Purchases  (US-012)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-012 Feed Purchase")
    class FeedPurchase {

        @Test
        @DisplayName("EP-VEC-01 | Valid purchase → 201 Created")
        void validPurchaseReturnsCreated() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     50);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("EP-VEC-02 | Purchase increments feed type currentStock")
        void purchaseIncreasesStock() throws Exception {
            int oldStock = getStockForCurrentFeedType();
            purchaseSacks(40);
            int stock = getStockForCurrentFeedType();
            Assertions.assertEquals(oldStock + 40, stock,
                    "currentStock should equal purchased quantity added to old stock");
        }

        @Test
        @DisplayName("EP-IEC-01 | sacksQty = 0 → 400 Bad Request")
        void zeroSackQtyRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     0);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-02 | Negative sacksQty → 400 Bad Request")
        void negativeSackQtyRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     -10);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-03 | costPerSack = 0 → 400 Bad Request")
        void zeroCostRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  0);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-04 | Missing supplierId → 400 Bad Request")
        void missingSupplierRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("sacksQty",     20);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-05 | Missing feedTypeId → 400 Bad Request")
        void missingFeedTypeRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     20);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("BVA-01 | sacksQty = 1 (lower boundary) → 201 Created")
        void minOneSackBoundary() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     1);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("BVA-02 | costPerSack = 1 (minimum positive) → 201 Created")
        void minCostPerSackBoundary() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  1);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("EG-01 | Unknown feedTypeId (random UUID) → 400 Bad Request")
        void unknownFeedTypeRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   UUID.randomUUID().toString());
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

            mvc.perform(post("/api/feed/purchases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. Feed Usage  (US-013)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-013 Feed Usage")
    class FeedUsage {

        @Test
        @DisplayName("EP-VEC-03 | Valid DAY usage within stock → 201 Created")
        void validDayUsageCreated() throws Exception {
            purchaseSacks(100);

            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  20);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "DAY");

            recordUsage(b, 201);
        }

        @Test
        @DisplayName("EP-VEC-04 | Valid NIGHT usage within stock → 201 Created")
        void validNightUsageCreated() throws Exception {
            purchaseSacks(100);

            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  10);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "NIGHT");

            recordUsage(b, 201);
        }

        @Test
        @DisplayName("EP-IEC-06 | shift absent → 400 Bad Request")
        void missingShiftRejected() throws Exception {
            purchaseSacks(100);

            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  10);
            b.put("recordDate", "2025-01-20");

            recordUsage(b, 400);
        }

        @Test
        @DisplayName("EP-IEC-07 | Invalid shift value → 400 Bad Request")
        void invalidShiftRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  5);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "AFTERNOON");

            recordUsage(b, 400);
        }

        @Test
        @DisplayName("EP-IEC-08 | sacksUsed = 0 → 400 Bad Request")
        void zeroSacksUsedRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  0);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "DAY");

            recordUsage(b, 400);
        }

        @Test
        @DisplayName("BVA-03 | sacksUsed exactly equals stock → 201 (exact depletion), then stock = 0")
        void usageExactlyEqualsStock() throws Exception {
            purchaseSacks(10);

            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  10);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "DAY");

            recordUsage(b, 201);

            int stock = getStockForCurrentFeedType();
            Assertions.assertEquals(0, stock, "Stock must be 0 after exact depletion");
        }

        @Test
        @DisplayName("EG-02 | Duplicate (same flock+feedType+date+shift) → 200 OK (upsert)")
        void duplicateUsageSameDayUpserts() throws Exception {
            purchaseSacks(100);

            Map<String, Object> b = new HashMap<>();
            b.put("flockId",    flockId);
            b.put("feedTypeId", feedTypeId);
            b.put("sacksUsed",  5);
            b.put("recordDate", "2025-01-20");
            b.put("shift",      "DAY");

            recordUsage(b, 201);

            recordUsage(b, 200);
        }

        @Test
        @DisplayName("EG-03 | DAY and NIGHT entries same flock+feedType+date → both 201 (different shift key)")
        void dayAndNightEntriesCoexist() throws Exception {
            purchaseSacks(100);

            Map<String, Object> day = new HashMap<>();
            day.put("flockId",    flockId);
            day.put("feedTypeId", feedTypeId);
            day.put("sacksUsed",  5);
            day.put("recordDate", "2025-01-21");
            day.put("shift",      "DAY");

            Map<String, Object> night = new HashMap<>();
            night.put("flockId",    flockId);
            night.put("feedTypeId", feedTypeId);
            night.put("sacksUsed",  3);
            night.put("recordDate", "2025-01-21");
            night.put("shift",      "NIGHT");

            recordUsage(day, 201);

            recordUsage(night, 201);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  3. Feed Sales  (US-014)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-014 Feed Sack Sales")
    class FeedSale {

        @Test
        @DisplayName("EP-VEC-05 | Valid sale → 201 Created")
        void validFeedSaleCreated() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("saleDate",     "2025-03-01");
            b.put("sacksQty",     10);
            b.put("pricePerSack", 350);
            b.put("buyerName",    "Ahmed Traders");

            recordSale(b, 201);
        }

        @Test
        @DisplayName("BVA-04 | sacksQty = 1 (minimum) → 201 Created")
        void minOneSackSale() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("saleDate",     "2025-03-01");
            b.put("sacksQty",     1);
            b.put("pricePerSack", 350);
            b.put("buyerName",    "Single Sack Buyer");

            recordSale(b, 201);
        }

        @Test
        @DisplayName("GET /api/feed/sales → 200 OK, array body")
        void listFeedSales() throws Exception {
            mvc.perform(get("/api/feed/sales"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  4. Feed Stock Report  (US-015)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("US-015 Feed Stock Report")
    class FeedStockReport {

        @Test
        @DisplayName("EP-VEC-06 | GET /api/feed-types → 200 OK, array body")
        void getFeedTypes() throws Exception {
            mvc.perform(get("/api/feed-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-07 | Purchased feed type shows correct currentStock")
        void purchasedFeedTypeAppearsInStockList() throws Exception {
            int oldStock = getStockForCurrentFeedType();
            purchaseSacks(30);
            int stock = getStockForCurrentFeedType();
            Assertions.assertEquals(30 + oldStock, stock,
                    "currentStock must equal number of sacks purchased added to old stock");
        }

        @Test
        @DisplayName("EP-VEC-08 | GET /api/feed-types/low-stock → 200 OK, array of low-stock types")
        void getLowStockFeedTypes() throws Exception {
            mvc.perform(get("/api/feed-types/low-stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-09 | POST /api/feed-types/upsert existing name → 200 OK (update threshold)")
        void upsertExistingFeedTypeUpdatesThreshold() throws Exception {
            String resp = mvc.perform(get("/api/feed-types"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String typeName = null;
            for (com.fasterxml.jackson.databind.JsonNode node : om.readTree(resp)) {
                if (feedTypeId.equals(node.get("feedTypeId").asText())) {
                    typeName = node.get("name").asText();
                    break;
                }
            }
            Assertions.assertNotNull(typeName, "Feed type must be found in list");

            Map<String, Object> b = new HashMap<>();
            b.put("name",         typeName);
            b.put("minThreshold", 10);

            mvc.perform(post("/api/feed-types/upsert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.minThreshold").value(10));
        }

        @Test
        @DisplayName("EP-IEC-09 | POST /api/feed-types/upsert with blank name → 400 Bad Request")
        void blankFeedTypeNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "");
            b.put("minThreshold", 5);

            mvc.perform(post("/api/feed-types/upsert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }
    }
}