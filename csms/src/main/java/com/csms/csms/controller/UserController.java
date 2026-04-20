package com.csms.csms.controller;

import com.csms.csms.entity.User;
import com.csms.csms.entity.UserRole;
import com.csms.csms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")  // Allow HTML to call this from browser
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/users - Get all users
     * Called by: HTML dashboard to show user list
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id} - Get single user by ID
     * Called by: HTML to get user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/users/search/email?email=john@example.com
     * Called by: Login page to find user by email
     * YOUR findByEmail() method is used here!
     */
    @GetMapping("/search/email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * POST /api/users - Create new user
     * Called by: HTML form when user clicks "Add User" button
     * Request body: { "username": "john_doe", "email": "john@example.com", "role": "MANAGER" }
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        User user = new User(request.getUsername(), request.getEmail(), request.getRole());
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    /**
     * PUT /api/users/{id} - Update user
     * Called by: HTML when user clicks "Edit" button
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody UserRequest request) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            user.setIsActive(request.getIsActive());
            User updated = userRepository.save(user);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * DELETE /api/users/{id} - Delete user
     * Called by: HTML when user clicks "Delete" button
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/users/role/ADMIN - Get all users with specific role
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userRepository.findByRoleAndIsActiveTrue(role);
        return ResponseEntity.ok(users);
    }
}

/**
 * Request DTO - What HTML sends to backend
 */
class UserRequest {
    private String username;
    private String email;
    private UserRole role;
    private Boolean isActive = true;

    // Constructors
    public UserRequest() {}

    public UserRequest(String username, String email, UserRole role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}