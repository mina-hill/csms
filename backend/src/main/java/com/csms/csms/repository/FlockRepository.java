package com.csms.csms.repository;

import com.csms.csms.entity.Flock;
import com.csms.csms.entity.FlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlockRepository extends JpaRepository<Flock, UUID> {

    /**
     * US-001 / US-003: Filter flocks by lifecycle status.
     * Used by the dashboard ("Active Flocks" count) and
     * by the legacy JS fillFlockSelect() which only shows ACTIVE flocks.
     *
     * @param status ACTIVE or CLOSED
     */
    List<Flock> findByStatus(FlockStatus status);

    /**
     * Used by the controller to prevent duplicate flock codes
     * and by the frontend flock selector dropdowns.
     */
    Optional<Flock> findByFlockCode(String flockCode);

    /**
     * Dashboard stat: count active flocks without loading all records.
     */
    long countByStatus(FlockStatus status);

    /**
     * Check whether a flock code is already taken before assigning.
     * Called in the service layer during POST /api/flocks.
     */
    boolean existsByFlockCode(String flockCode);
}
