package com.example.securingweb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "Relationships")
public class Relationship {
    @DynamoDBHashKey
    private String left;
    @DynamoDBRangeKey
    private String right;

    public Relationship() {}

    @Override
    public String toString() {
        return "Relationship{" +
                "left='" + left + '\'' +
                ", right='" + right + '\'' +
                '}';
    }

}