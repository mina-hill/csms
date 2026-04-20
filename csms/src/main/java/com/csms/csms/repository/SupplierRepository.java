package com.csms.csms.repository;

import com.csms.csms.entity.Supplier;
import com.csms.csms.entity.SupplierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    /**
     * Find all active suppliers
     * US-007: Used to display supplier list
     * 
     * @return List of active suppliers
     */
    List<Supplier> findByIsActive(Boolean isActive);

    /**
     * Find supplier by unique name
     * US-007: Duplicate check on supplier name
     * 
     * @param name the supplier name
     * @return Optional containing supplier if found
     */
    Optional<Supplier> findByName(String name);

    /**
     * Find all active suppliers by type
     * US-007: Filter suppliers by category (FEED, MEDICINE, etc.)
     * 
     * @param supplierType the type of supplier
     * @return List of active suppliers of specified type
     */
    List<Supplier> findBySupplierTypeAndIsActive(SupplierType supplierType, Boolean isActive);

    /**
     * Find all suppliers by type (active and inactive)
     * 
     * @param supplierType the type of supplier
     * @return List of all suppliers of specified type
     */
    List<Supplier> findBySupplierType(SupplierType supplierType);

    /**
     * Check if supplier name exists
     * US-007: Validation for duplicate name
     * 
     * @param name the supplier name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Get all suppliers ordered by name
     * 
     * @return List of all suppliers sorted by name
     */
    List<Supplier> findAllByOrderByNameAsc();

    /**
     * Get active suppliers ordered by name
     * 
     * @return List of active suppliers sorted by name
     */
    List<Supplier> findByIsActiveTrueOrderByNameAsc();
}