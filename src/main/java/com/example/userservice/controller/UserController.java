package com.example.userservice.controller;

import com.example.userservice.model.User;
import com.example.userservice.service.UserEventProducer;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserEventProducer eventProducer;

    @Autowired
    public UserController(UserService userService, UserEventProducer eventProducer) {
        this.userService = userService;
        this.eventProducer = eventProducer;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        List<User> users = userService.findAll(limit, offset);
        int total = userService.count();

        Map<String, Object> response = new HashMap<>();
        response.put("data", users);
        response.put("total", total);
        response.put("limit", limit);
        response.put("offset", offset);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.create(user);
        eventProducer.publishUserCreatedEvent(createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userService.findById(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody User user) {

        User updatedUser = userService.update(userId, user);
        if (updatedUser != null) {
            eventProducer.publishUserUpdatedEvent(updatedUser);
            return ResponseEntity.ok(updatedUser);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        User deletedUser = userService.delete(userId);
        if (deletedUser != null) {
            eventProducer.publishUserDeletedEvent(deletedUser);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{userId}/profile")
    public ResponseEntity<User> updateProfile(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {

        User updatedUser = userService.updateProfile(userId, updates);
        if (updatedUser != null) {
            eventProducer.publishProfileUpdatedEvent(updatedUser, updates);
            return ResponseEntity.ok(updatedUser);
        }
        return ResponseEntity.notFound().build();
    }
}
