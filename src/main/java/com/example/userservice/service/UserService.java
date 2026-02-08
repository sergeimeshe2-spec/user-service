package com.example.userservice.service;

import com.example.userservice.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserService() {
        // Initialize with sample data
        User user1 = new User();
        user1.setUserId("1");
        user1.setName("Ivan Ivanov");
        user1.setEmail("ivan@example.com");
        user1.setPhoneNumber("+79001234567");
        user1.setStatus("ACTIVE");

        User user2 = new User();
        user2.setUserId("2");
        user2.setName("Petr Petrov");
        user2.setEmail("petr@example.com");
        user2.setPhoneNumber("+79007654321");
        user2.setStatus("ACTIVE");

        users.put("1", user1);
        users.put("2", user2);
    }

    public List<User> findAll(int limit, int offset) {
        return users.values().stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public int count() {
        return users.size();
    }

    public User findById(String userId) {
        return users.get(userId);
    }

    public User create(User user) {
        if (user.getUserId() == null) {
            user.setUserId(String.valueOf(System.currentTimeMillis()));
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        users.put(user.getUserId(), user);
        return user;
    }

    public User update(String userId, User user) {
        User existing = users.get(userId);
        if (existing != null) {
            existing.setName(user.getName());
            existing.setEmail(user.getEmail());
            existing.setPhoneNumber(user.getPhoneNumber());
            existing.setStatus(user.getStatus());
            existing.setUpdatedAt(LocalDateTime.now());
            return existing;
        }
        return null;
    }

    public User updateProfile(String userId, Map<String, Object> updates) {
        User existing = users.get(userId);
        if (existing != null) {
            if (updates.containsKey("name")) {
                existing.setName((String) updates.get("name"));
            }
            if (updates.containsKey("phoneNumber")) {
                existing.setPhoneNumber((String) updates.get("phoneNumber"));
            }
            existing.setUpdatedAt(LocalDateTime.now());
            return existing;
        }
        return null;
    }

    public User delete(String userId) {
        return users.remove(userId);
    }
}
