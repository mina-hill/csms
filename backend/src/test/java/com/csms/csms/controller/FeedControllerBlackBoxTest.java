package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.csms.csms.repository.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Black-Box Test Suite Ã¢â‚¬â€ Feed Management Module
 *
 * Techniques applied:
 *   EP  = Equivalence Partitioning
 *   BVA = Boundary Value Analysis
 *   EG  = Error Guessing
 *
 * Endpoints under test:
 *   POST /api/feed/purchases     Ã¢â‚¬â€œ stock in          (US-012)
 *   POST /api/feed/usage         Ã¢â‚¬â€œ stock out          (US-013)
 *   POST /api/feed/sales         Ã¢â‚¬â€œ surplus sack sale  (US-014)
 *   POST /api/feed-types/upsert  Ã¢â‚¬â€œ create/update feed type
 *   GET  /api/feed-types         Ã¢â‚¬â€œ list all types / stock report (US-015)
 *
 * FIX: Removed @Transactional from the class.
 *      The DB stock counter is updated by a trigger that runs in its own
 *      transaction. With @Transactional on the test, the trigger commits its
 *      sub-transaction but the JPA entity read inside the same rolled-back test
 *      transaction still sees the old (pre-trigger) value Ã¢â€ â€™ currentStock = 0.
 *      Removing @Transactional lets every operation commit normally so stock
 *      reads reflect real state.
 *
 *      @AfterEach deletes ONLY the rows created by this test run, identified
 *      by the IDs collected in createdPurchaseIds / createdUsageIds /
 *      createdSaleIds, plus the setUp() supplier, feedType, and flock.
 *      Pre-existing production data is never touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Feed Management Ã¢â‚¬â€ Black-Box Tests")
class FeedControllerBlackBoxTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // Repositories injected solely for @AfterEach cleanup
    @Autowired FeedUsageRepository    feedUsageRepository;
    @Autowired FeedSaleRepository     feedSaleRepository;
    @Autowired FeedPurchaseRepository feedPurchaseRepository;
    @Autowired FeedTypeRepository     feedTypeRepository;
    @Autowired FlockRepository        flockRepository;
    @Autowired SupplierRepository     supplierRepository;
    @Autowired UserRepository         userRepository;

    private String supplierId;
    private String feedTypeId;
    private String flockId;
    private String actorId;

    private static final String AUTH_HEADER = CsmsAccessHelper.USER_ID_HEADER;

    // Tracks IDs created during each test so tearDown deletes only those rows
    private final List<UUID> createdPurchaseIds = new ArrayList<>();
    private final List<UUID> createdUsageIds    = new ArrayList<>();
    private final List<UUID> createdSaleIds     = new ArrayList<>();

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    //  Setup / Teardown
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @BeforeEach
    void setUp() throws Exception {
        // Create a test user with ADMIN role to satisfy both 'requireFinancial' and 'requireShedManager'
        com.csms.csms.entity.User actor = new com.csms.csms.entity.User();
        actor.setUsername("testadmin_" + UUID.randomUUID().toString().substring(0, 8));
        actor.setEmail("admin_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        actor.setRole(com.csms.csms.entity.UserRole.ADMIN);
        actor.setIsActive(true);
        actor = userRepository.saveAndFlush(actor);
        actorId = actor.getUserId().toString();

        supplierId = createSupplier("Feed Supplier Co " + UUID.randomUUID());
        feedTypeId = upsertFeedType("Starter_" + UUID.randomUUID(), 5);
        flockId    = createFlock("Ross 308", 5000, "2025-01-01");
        createdPurchaseIds.clear();
        createdUsageIds.clear();
        createdSaleIds.clear();
    }

    /**
     * Deletes only the rows this test created, in FK-safe order.
     * Pre-existing production data is never touched.
     */
    @AfterEach
    void tearDown() {
        createdUsageIds.forEach(feedUsageRepository::deleteById);
        createdSaleIds.forEach(feedSaleRepository::deleteById);
        createdPurchaseIds.forEach(feedPurchaseRepository::deleteById);
        if (feedTypeId != null) {
            feedPurchaseRepository.deleteAll(
                    feedPurchaseRepository.findByFeedTypeId(UUID.fromString(feedTypeId)));
            feedTypeRepository.deleteById(UUID.fromString(feedTypeId));
        }
        if (flockId != null) {
            flockRepository.deleteById(UUID.fromString(flockId));
        }
        if (supplierId != null) {
            supplierRepository.deleteById(UUID.fromString(supplierId));
        }
        if (actorId != null) {
            userRepository.deleteById(UUID.fromString(actorId));
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ helpers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private String createSupplier(String name) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name", name);
        b.put("supplierType", "FEED");
        String resp = mvc.perform(post("/api/suppliers")
                .header(AUTH_HEADER, actorId)
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
                .header(AUTH_HEADER, actorId)
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
                .header(AUTH_HEADER, actorId)
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
        String resp = mvc.perform(post("/api/feed/purchases")
                .header(AUTH_HEADER, actorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(b)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        createdPurchaseIds.add(UUID.fromString(om.readTree(resp).get("purchaseId").asText()));
    }

    /** Records usage and tracks the ID for teardown. Returns the response status. */
    private void recordUsage(Map<String, Object> body, int expectedStatus) throws Exception {
        String resp = mvc.perform(post("/api/feed/usage")
                .header(AUTH_HEADER, actorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        if ((expectedStatus == 200 || expectedStatus == 201) && !resp.isBlank()) {
            com.fasterxml.jackson.databind.JsonNode n = om.readTree(resp);
            if (n.has("usageId")) createdUsageIds.add(UUID.fromString(n.get("usageId").asText()));
        }
    }

    /** Records a feed sale and tracks the ID for teardown. */
    private void recordSale(Map<String, Object> body, int expectedStatus) throws Exception {
        String resp = mvc.perform(post("/api/feed/sales")
                .header(AUTH_HEADER, actorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        if (expectedStatus == 201 && !resp.isBlank()) {
            com.fasterxml.jackson.databind.JsonNode n = om.readTree(resp);
            if (n.has("saleId")) createdSaleIds.add(UUID.fromString(n.get("saleId").asText()));
        }
    }

    /** Returns currentStock for the feedTypeId created in setUp(). */
    private int getStockForCurrentFeedType() throws Exception {
        String resp = mvc.perform(get("/api/feed-types")
                .header(AUTH_HEADER, actorId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (com.fasterxml.jackson.databind.JsonNode node : om.readTree(resp)) {
            if (feedTypeId.equals(node.get("feedTypeId").asText())) {
                return node.get("currentStock").asInt();
            }
        }
        return -1;
    }


    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    //  1. Feed Purchases  (US-012)
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    @Nested
    @DisplayName("US-012 Feed Purchase")
    class FeedPurchase {

        @Test
        @DisplayName("EP-VEC-01 | Valid purchase Ã¢â€ â€™ 201 Created")
        void validPurchaseReturnsCreated() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     50);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
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
        @DisplayName("EP-IEC-01 | sacksQty = 0 Ã¢â€ â€™ 400 Bad Request")
        void zeroSackQtyRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     0);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-02 | Negative sacksQty Ã¢â€ â€™ 400 Bad Request")
        void negativeSackQtyRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     -10);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-03 | costPerSack = 0 Ã¢â€ â€™ 400 Bad Request")
        void zeroCostRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  0);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-04 | Missing supplierId Ã¢â€ â€™ 400 Bad Request")
        void missingSupplierRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("sacksQty",     20);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EP-IEC-05 | Missing feedTypeId Ã¢â€ â€™ 400 Bad Request")
        void missingFeedTypeRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     20);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("BVA-01 | sacksQty = 1 (lower boundary) Ã¢â€ â€™ 201 Created")
        void minOneSackBoundary() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     1);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("BVA-02 | costPerSack = 1 (minimum positive) Ã¢â€ â€™ 201 Created")
        void minCostPerSackBoundary() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   feedTypeId);
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  1);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("EG-01 | Unknown feedTypeId (random UUID) Ã¢â€ â€™ 400 Bad Request")
        void unknownFeedTypeRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("feedTypeId",   UUID.randomUUID().toString());
            b.put("supplierId",   supplierId);
            b.put("sacksQty",     10);
            b.put("costPerSack",  300);
            b.put("purchaseDate", "2025-02-01");

                mvc.perform(post("/api/feed/purchases")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    //  2. Feed Usage  (US-013)
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    @Nested
    @DisplayName("US-013 Feed Usage")
    class FeedUsage {

        @Test
        @DisplayName("EP-VEC-03 | Valid DAY usage within stock Ã¢â€ â€™ 201 Created")
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
        @DisplayName("EP-VEC-04 | Valid NIGHT usage within stock Ã¢â€ â€™ 201 Created")
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
        @DisplayName("EP-IEC-06 | shift absent Ã¢â€ â€™ 400 Bad Request")
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
        @DisplayName("EP-IEC-07 | Invalid shift value Ã¢â€ â€™ 400 Bad Request")
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
        @DisplayName("EP-IEC-08 | sacksUsed = 0 Ã¢â€ â€™ 400 Bad Request")
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
        @DisplayName("BVA-03 | sacksUsed exactly equals stock Ã¢â€ â€™ 201 (exact depletion), then stock = 0")
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
        @DisplayName("EG-02 | Duplicate (same flock+feedType+date+shift) Ã¢â€ â€™ 200 OK (upsert)")
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
        @DisplayName("EG-03 | DAY and NIGHT entries same flock+feedType+date Ã¢â€ â€™ both 201 (different shift key)")
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

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    //  3. Feed Sales  (US-014)
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    @Nested
    @DisplayName("US-014 Feed Sack Sales")
    class FeedSale {

        @Test
        @DisplayName("EP-VEC-05 | Valid sale Ã¢â€ â€™ 201 Created")
        void validFeedSaleCreated() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("saleDate",     "2025-03-01");
            b.put("sacksQty",     10);
            b.put("pricePerSack", 350);
            b.put("buyerName",    "Ahmed Traders");

            recordSale(b, 201);
        }

        @Test
        @DisplayName("BVA-04 | sacksQty = 1 (minimum) Ã¢â€ â€™ 201 Created")
        void minOneSackSale() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("saleDate",     "2025-03-01");
            b.put("sacksQty",     1);
            b.put("pricePerSack", 350);
            b.put("buyerName",    "Single Sack Buyer");

            recordSale(b, 201);
        }

        @Test
        @DisplayName("GET /api/feed/sales Ã¢â€ â€™ 200 OK, array body")
        void listFeedSales() throws Exception {
                mvc.perform(get("/api/feed/sales")
                        .header(AUTH_HEADER, actorId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    //  4. Feed Stock Report  (US-015)
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    @Nested
    @DisplayName("US-015 Feed Stock Report")
    class FeedStockReport {

        @Test
        @DisplayName("EP-VEC-06 | GET /api/feed-types Ã¢â€ â€™ 200 OK, array body")
        void getFeedTypes() throws Exception {
                mvc.perform(get("/api/feed-types")
                        .header(AUTH_HEADER, actorId))
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
        @DisplayName("EP-VEC-08 | GET /api/feed-types/low-stock Ã¢â€ â€™ 200 OK, array of low-stock types")
        void getLowStockFeedTypes() throws Exception {
                mvc.perform(get("/api/feed-types/low-stock")
                        .header(AUTH_HEADER, actorId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("EP-VEC-09 | POST /api/feed-types/upsert existing name Ã¢â€ â€™ 200 OK (update threshold)")
        void upsertExistingFeedTypeUpdatesThreshold() throws Exception {
                String resp = mvc.perform(get("/api/feed-types")
                        .header(AUTH_HEADER, actorId))
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
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.minThreshold").value(10));
        }

        @Test
        @DisplayName("EP-IEC-09 | POST /api/feed-types/upsert with blank name Ã¢â€ â€™ 400 Bad Request")
        void blankFeedTypeNameRejected() throws Exception {
            Map<String, Object> b = new HashMap<>();
            b.put("name",         "");
            b.put("minThreshold", 5);

                mvc.perform(post("/api/feed-types/upsert")
                        .header(AUTH_HEADER, actorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(b)))
                    .andExpect(status().isBadRequest());
        }
    }
}

