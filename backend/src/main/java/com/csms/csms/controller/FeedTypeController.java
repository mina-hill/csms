package com.csms.csms.controller;

import com.csms.csms.auth.CsmsAccessHelper;
import com.csms.csms.entity.FeedType;
import com.csms.csms.repository.FeedTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/feed-types")
@CrossOrigin(origins = "*")
public class FeedTypeController {

    @Autowired
    private FeedTypeRepository feedTypeRepository;

    @Autowired
    private CsmsAccessHelper accessHelper;

    @GetMapping
    public ResponseEntity<List<FeedType>> getAllFeedTypes() {
        return ResponseEntity.ok(feedTypeRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<FeedType>> getLowStockFeedTypes(@RequestParam(required = false) Integer threshold) {
        if (threshold != null) {
            return ResponseEntity.ok(feedTypeRepository.findByCurrentStockLessThan(threshold));
        }
        return ResponseEntity.ok(feedTypeRepository.findByCurrentStockLessThanMinThreshold());
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsertFeedType(
            @RequestBody FeedTypeUpsertRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("name is required.");
        }
        String normalizedName = request.getName().trim();
        int threshold = request.getMinThreshold() != null ? request.getMinThreshold() : 5;
        if (threshold < 0) threshold = 0;
        final int normalizedThreshold = threshold;

        Optional<FeedType> existing = feedTypeRepository.findByName(normalizedName);
        FeedType feedType = existing.orElseGet(() -> new FeedType(normalizedName, normalizedThreshold));
        if (existing.isPresent()) {
            feedType.setMinThreshold(normalizedThreshold);
        }
        FeedType saved = feedTypeRepository.save(feedType);
        return ResponseEntity.status(existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/threshold")
    public ResponseEntity<?> updateThreshold(
            @PathVariable UUID id,
            @RequestBody FeedThresholdRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getMinThreshold() == null || request.getMinThreshold() < 0) {
            return ResponseEntity.badRequest().body("minThreshold must be >= 0.");
        }
        Optional<FeedType> existing = feedTypeRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        FeedType feedType = existing.get();
        feedType.setMinThreshold(request.getMinThreshold());
        return ResponseEntity.ok(feedTypeRepository.save(feedType));
    }

    @PatchMapping("/stock")
    public ResponseEntity<?> adjustStock(
            @RequestBody FeedStockAdjustRequest request,
            @RequestHeader(value = CsmsAccessHelper.USER_ID_HEADER, required = false) String actorId) {
        accessHelper.requireShedManagerOrAdminOrThrow(actorId);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("name is required.");
        }
        if (request.getDelta() == null || request.getDelta() == 0) {
            return ResponseEntity.badRequest().body("delta must be non-zero.");
        }
        String normalizedName = request.getName().trim();
        FeedType feedType = feedTypeRepository.findByName(normalizedName).orElse(null);
        if (feedType == null) {
            if (request.getDelta() < 0) {
                return ResponseEntity.badRequest().body("Cannot decrement stock for non-existing feed type.");
            }
            feedType = new FeedType(normalizedName, request.getMinThreshold() != null ? request.getMinThreshold() : 5);
            feedType.setCurrentStock(0);
        }
        int nextStock = (feedType.getCurrentStock() == null ? 0 : feedType.getCurrentStock()) + request.getDelta();
        if (nextStock < 0) {
            return ResponseEntity.badRequest().body("Insufficient feed stock.");
        }
        feedType.setCurrentStock(nextStock);
        if (request.getMinThreshold() != null && request.getMinThreshold() >= 0) {
            feedType.setMinThreshold(request.getMinThreshold());
        }
        return ResponseEntity.ok(feedTypeRepository.save(feedType));
    }
}

class FeedTypeUpsertRequest {
    private String name;
    private Integer minThreshold;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
}

class FeedThresholdRequest {
    private Integer minThreshold;

    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
}

class FeedStockAdjustRequest {
    private String name;
    private Integer delta;
    private Integer minThreshold;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
}
