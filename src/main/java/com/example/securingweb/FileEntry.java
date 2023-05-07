package com.example.securingweb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import lombok.Data;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@DynamoDBTable(tableName = "Files")
public class FileEntry {

    private Long id;
    @DynamoDBRangeKey(attributeName = "filename")
    private String fileName;
    @DynamoDBHashKey(attributeName = "username")
    private String userName;
    private Date dateUploaded;
    @DynamoDBTypeConvertedEnum
    private Status status;

    public FileEntry() {};
    public FileEntry(Long id, String fileName, String userName, Date dateUploaded, Status status) {
        this.id = id;
        this.fileName = fileName;
        this.userName = userName;
        this.dateUploaded = dateUploaded;
        this.status = status;
    }

    public enum Status {
        NOT_STARTED,
        PROCESSING,
        FAILED,
        FINISHED
    }
}