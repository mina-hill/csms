package com.csms.csms.auth;

import com.csms.csms.entity.User;
import com.csms.csms.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public login: identifies user by email (no password in current schema).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;

    @Value("${csms.auth.login-diagnostics:false}")
    private boolean loginDiagnostics;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required (JSON: {\"email\":\"...\"})"));
        }
        String raw = body.resolvedEmail();
        String email = EmailLoginNormalizer.normalize(raw);
        if (!StringUtils.hasText(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        if (log.isDebugEnabled()) {
            log.debug("Login: normalized length={}, codePoint0=0x{}",
                    email.length(), email.isEmpty() ? "0" : Integer.toHexString(email.codePointAt(0)));
        }
        Optional<User> u = findUserForLogin(email);
        if (u.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Login: no row for email after HQL+SpringData+native fallbacks (length={}).", email.length());
            }
            if (loginDiagnostics) {
                long count = userRepository.count();
                Map<String, Object> dbg = new LinkedHashMap<>();
                dbg.put("message", "No user with this email");
                dbg.put("userCount", count);
                dbg.put("note", "userCount=0 means this API uses an empty or different database than in Supabase UI. If count>0, the email text does not match any row (check spaces/typos in DB).");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(dbg);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No user with this email"));
        }
        User user = u.get();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account is inactive"));
        }
        return ResponseEntity.ok(LoginUserDto.fromEntity(user));
    }

    private Optional<User> findUserForLogin(String email) {
        Optional<User> o = userRepository.findByEmailForLogin(email);
        if (o.isPresent()) return o;
        o = userRepository.findByEmailIgnoreCase(email);
        if (o.isPresent()) return o;
        o = userRepository.findByEmail(email);
        if (o.isPresent()) return o;
        try {
            o = userRepository.findByEmailForLoginNative(email);
        } catch (Exception e) {
            log.warn("Login native query fallback failed: {}", e.getMessage());
        }
        return o;
    }
}

class LoginRequest {
    @JsonProperty("email")
    @JsonAlias({ "userEmail", "user_email", "UserEmail" })
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    String resolvedEmail() {
        if (StringUtils.hasText(email)) return email;
        return "";
    }
}

class LoginUserDto {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private boolean active;

    public static LoginUserDto fromEntity(User u) {
        LoginUserDto d = new LoginUserDto();
        d.userId = u.getUserId();
        d.username = u.getUsername();
        d.email = u.getEmail();
        d.role = u.getRole() != null ? u.getRole().name() : null;
        d.active = Boolean.TRUE.equals(u.getIsActive());
        return d;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
