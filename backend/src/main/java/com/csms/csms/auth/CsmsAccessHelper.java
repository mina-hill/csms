package com.csms.csms.auth;

import com.csms.csms.entity.User;
import com.csms.csms.entity.UserRole;
import com.csms.csms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped role checks. Client sends {@value #USER_ID_HEADER} (logged-in user id)
 * on mutating API calls; replace with JWT/session in production.
 */
@Component
public class CsmsAccessHelper {

    public static final String USER_ID_HEADER = "X-CSMS-User-Id";

    private final UserRepository userRepository;

    public CsmsAccessHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findActor(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(userIdHeader.trim());
            return userRepository.findById(id).filter(u -> Boolean.TRUE.equals(u.getIsActive()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static boolean isShedManagerOrAdmin(User u) {
        UserRole r = u.getRole();
        return r == UserRole.ADMIN || r == UserRole.MANAGER;
    }

    public static boolean isAccountantOrShedOrAdmin(User u) {
        UserRole r = u.getRole();
        return r == UserRole.ADMIN || r == UserRole.MANAGER || r == UserRole.ACCOUNTANT;
    }

    /** Flocks, daily ops, user admin, workers, catalog, weekly record, system audit, etc. */
    public void requireShedManagerOrAdminOrThrow(String userIdHeader) {
        User u = findActor(userIdHeader).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid session"));
        if (!isShedManagerOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires shed manager or admin");
        }
    }

    /** Expenses, sales, purchases, payroll — accountant, manager, or admin. */
    public void requireFinancialOrThrow(String userIdHeader) {
        User u = findActor(userIdHeader).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid session"));
        if (!isAccountantOrShedOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this role");
        }
    }
}
