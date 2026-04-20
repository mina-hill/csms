package com.csms.csms.repository;

import com.csms.csms.entity.FeedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedTypeRepository extends JpaRepository<FeedType, UUID> {

    /**
     * Find all feed types with stock below threshold
     * US-015: Low-stock warning shown when stock < minThreshold
     * 
     * @param threshold the stock level to compare against
     * @return List of feed types with currentStock < threshold
     */
    List<FeedType> findByCurrentStockLessThan(Integer threshold);

    /**
     * Find feed type by unique name
     * 
     * @param name the feed type name (starter, grower, finisher, etc.)
     * @return Optional containing feed type if found
     */
    Optional<FeedType> findByName(String name);

    /**
     * Check if feed type exists by name
     * 
     * @param name the feed type name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find all feed types ordered by name
     * 
     * @return List of feed types sorted by name
     */
    List<FeedType> findAllByOrderByNameAsc();

    /**
     * Find feed types with stock below their minimum threshold
     * US-015: Dashboard warning indicator
     * 
     * @return List of feed types needing reorder
     */
    @Query("SELECT f FROM FeedType f WHERE f.currentStock < f.minThreshold")
    List<FeedType> findByCurrentStockLessThanMinThreshold();
}