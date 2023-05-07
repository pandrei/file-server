package com.example.securingweb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.example.securingweb.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final DynamoDBMapper dynamoDBMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserRepository(AmazonDynamoDB amazonDynamoDB, BCryptPasswordEncoder passwordEncoder) {
        this.dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
        this.passwordEncoder = passwordEncoder;
    }

    public void save(User user) {

        try {
            logger.info("Added user {}", user);
            dynamoDBMapper.save(user);
        } catch (ConditionalCheckFailedException e) {
            logger.error("Username already exists: {}", user.getUsername(), e);
        }
    }

    public User findByUsername(String username) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<User> users = dynamoDBMapper.scan(User.class, scanExpression);

        List<User> matchingUsers = users.stream()
                .filter(user -> user.getUsername().equals(username))
                .collect(Collectors.toList());

        return matchingUsers.isEmpty() ? null : matchingUsers.get(0);
    }

    public Optional<User> findById(String username) {
        User user = dynamoDBMapper.load(User.class, username);
        return Optional.ofNullable(user);
    }
}