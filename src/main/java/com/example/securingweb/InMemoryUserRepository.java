package com.example.securingweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryUserRepository {
    private final Map<String, User> users = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(InMemoryUserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private long nextId = 1L;


    public void save(User user) {
        user.setId(nextId++);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        logger.info("Adding user: {}", user);
        users.put(user.getUsername(), user);
    }

    public User findByUsername(String username) {
        return users.get(username);
    }
}
