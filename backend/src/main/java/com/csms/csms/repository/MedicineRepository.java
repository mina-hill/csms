package com.csms.csms.repository;

import com.csms.csms.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    /**
     * Find all medicines with stock below threshold
     * US-019: Low-stock alert when stock < threshold
     * 
     * @param threshold the stock level to compare against
     * @return List of medicines with currentStock < threshold
     */
    List<Medicine> findByCurrentStockLessThan(Integer threshold);

    /**
     * Find medicine by unique name
     * 
     * @param name the medicine name
     * @return Optional containing medicine if found
     */
    Optional<Medicine> findByName(String name);

    /**
     * Check if medicine exists by name
     * 
     * @param name the medicine name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find all medicines ordered by name
     * 
     * @return List of medicines sorted by name
     */
    List<Medicine> findAllByOrderByNameAsc();

    /**
     * Find medicines with stock below their minimum threshold
     * US-019: Dashboard low-stock indicator
     * 
     * @return List of medicines needing reorder
     */
    @Query("SELECT m FROM Medicine m WHERE m.currentStock < m.minThreshold")
    List<Medicine> findByCurrentStockLessThanMinThreshold();
}