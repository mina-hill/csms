package com.csms.csms.repository;

import com.csms.csms.entity.User;
import com.csms.csms.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Case-insensitive match (PostgreSQL is case-sensitive for = on varchar).
     * Use for login and email lookup.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Login lookup: explicit HQL (avoids any Spring Data quirk) — lower + trim both sides.
     */
    @Query("SELECT u FROM User u WHERE LOWER(TRIM(BOTH FROM u.email)) = LOWER(TRIM(BOTH FROM :email))")
    Optional<User> findByEmailForLogin(@Param("email") String email);

    /**
     * Last resort: PostgreSQL native in case the entity manager maps differently.
     */
    @Query(
            value = "SELECT u.* FROM users u WHERE lower(trim(u.email::text)) = lower(trim(CAST(:email AS text)))",
            nativeQuery = true)
    Optional<User> findByEmailForLoginNative(@Param("email") String email);

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