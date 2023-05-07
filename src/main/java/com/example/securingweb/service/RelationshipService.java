package com.example.securingweb.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.example.securingweb.entity.Relationship;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RelationshipService {
    private final DynamoDBMapper dynamoDBMapper;
    public RelationshipService(AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }

    public List<Relationship> getRelationships() {
        return  dynamoDBMapper.scan(Relationship.class, new DynamoDBScanExpression());
    }
}