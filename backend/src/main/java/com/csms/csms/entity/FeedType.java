package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "feed_types")
public class FeedType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feed_type_id")
    private UUID feedTypeId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "min_threshold", nullable = false)
    private Integer minThreshold = 5;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "last_updated", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastUpdated;

    // Constructors
    public FeedType() {}

    public FeedType(String name) {
        this.name = name;
        this.minThreshold = 5;
        this.currentStock = 0;
    }

    public FeedType(String name, Integer minThreshold) {
        this.name = name;
        this.minThreshold = minThreshold;
        this.currentStock = 0;
    }

    // Getters & Setters
    public UUID getFeedTypeId() {
        return feedTypeId;
    }

    public void setFeedTypeId(UUID feedTypeId) {
        this.feedTypeId = feedTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Integer minThreshold) {
        this.minThreshold = minThreshold;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "FeedType{" +
                "feedTypeId=" + feedTypeId +
                ", name='" + name + '\'' +
                ", currentStock=" + currentStock +
                ", minThreshold=" + minThreshold +
                '}';
    }
}