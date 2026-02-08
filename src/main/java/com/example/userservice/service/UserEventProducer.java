package com.example.userservice.service;

import com.example.userservice.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.kafka.topics.user-created:user/created}")
    private String userCreatedTopic;

    @Value("${app.kafka.topics.user-updated:user/updated}")
    private String userUpdatedTopic;

    @Value("${app.kafka.topics.user-deleted:user/deleted}")
    private String userDeletedTopic;

    @Value("${app.kafka.topics.profile-updated:user/profile-updated}")
    private String profileUpdatedTopic;

    public void publishUserCreatedEvent(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "UserCreated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("data", user);

        sendEvent(userCreatedTopic, event);
    }

    public void publishUserUpdatedEvent(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "UserUpdated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("data", user);

        sendEvent(userUpdatedTopic, event);
    }

    public void publishUserDeletedEvent(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "UserDeleted");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("data", Map.of(
            "userId", user.getUserId(),
            "email", user.getEmail()
        ));

        sendEvent(userDeletedTopic, event);
    }

    public void publishProfileUpdatedEvent(User user, Map<String, Object> updates) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ProfileUpdated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("data", Map.of(
            "userId", user.getUserId(),
            "updates", updates
        ));

        sendEvent(profileUpdatedTopic, event);
    }

    private void sendEvent(String topic, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, json);
            log.info("Sent event to topic {}: {}", topic, event);
        } catch (Exception e) {
            log.error("Error sending event to topic {}: {}", topic, e.getMessage());
        }
    }
}
