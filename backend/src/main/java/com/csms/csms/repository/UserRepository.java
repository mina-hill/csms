package com.csms.csms.repository;

import com.csms.csms.entity.User;
import com.csms.csms.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email address
     * US-007: Used for login and user lookup
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by username
     * Used for unique username validation
     */
    Optional<User> findByUsername(String username);

    /**
     * Find all active users with a specific role
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Check if email exists (for duplicate validation)
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists (for duplicate validation)
     */
    boolean existsByUsername(String username);

    /**
     * Check if a specific user is active
     */
    boolean existsByUserIdAndIsActiveTrue(UUID userId);
}