package com.csms.csms.repository;

import com.csms.csms.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    /**
     * Find all active workers
     * US-023: Used to display active worker list
     * 
     * @return List of active workers
     */
    List<Worker> findByIsActive(Boolean isActive);

    /**
     * Find all active workers ordered by name
     * US-023: Display worker list sorted
     * 
     * @return List of active workers sorted by name
     */
    List<Worker> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find worker by name
     * 
     * @param name the worker name
     * @return Optional containing worker if found
     */
    Optional<Worker> findByName(String name);

    /**
     * Find workers by role
     * Example: "Shed Manager", "Accountant", "Shed Worker"
     * 
     * @param role the worker role
     * @return List of workers with specified role
     */
    List<Worker> findByRoleAndIsActive(String role, Boolean isActive);

    /**
     * Find all workers ordered by name
     * 
     * @return List of all workers sorted by name
     */
    List<Worker> findAllByOrderByNameAsc();

    /**
     * Get count of active workers
     * US-023: Useful for payroll statistics
     * 
     * @return Number of active workers
     */
    long countByIsActive(Boolean isActive);

    /**
     * Check if worker exists with specific name and role
     * US-023: Duplicate prevention (warn if same name + role exists)
     * 
     * @param name worker name
     * @param role worker role
     * @return true if exists, false otherwise
     */
    boolean existsByNameAndRole(String name, String role);
}