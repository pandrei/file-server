package com.example.securingweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class userStorageService {
    private static final Logger logger = LoggerFactory.getLogger(userStorageService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserDynamoDBRepository userDynamoDBRepository;
    private long nextId = 1L;


    public void save(User user) {
        user.setId(nextId++);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        logger.info("Adding user: {}", user);
        userDynamoDBRepository.save(user);
    }

    public User findByUsername(String username) {
        Optional<User> userOptional = userDynamoDBRepository.findById(username);
        return userOptional.orElse(null);
    }
}
