package com.example.securingweb.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.example.securingweb.entity.FileEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FileStorageService
{

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final AmazonS3 amazonS3;

    private final DynamoDBMapper dynamoDBMapper;
    private final AWSLambda lambda;
    public FileStorageService(AmazonS3 amazonS3, AWSLambda lambda, AmazonDynamoDB amazonDynamoDB) {
        this.amazonS3 = amazonS3;
        this.lambda = lambda;
        this.dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }
    public boolean createBucket(String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(new CreateBucketRequest(bucketName));
            logger.info("Created S3 bucket: {}", bucketName);
            return true;
        } else {
            logger.warn("S3 bucket already exists: {}", bucketName);
            return false;
        }
    }

    public ObjectListing listFiles(String bucketName) {
        return amazonS3.listObjects(bucketName);
    }
    public boolean bucketExists(String bucketName) {
        return amazonS3.doesBucketExistV2(bucketName);
    }

    public void startLambdaToTrackS3BucketEvents(String bucketName) {
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("bucketName", bucketName);
        ObjectMapper objectMapper = new ObjectMapper();
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing payload to JSON", e);
            return;
        }

        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName("event-fan-out")
                .withPayload(payloadJson);

        lambda.invoke(invokeRequest);
    }


    public boolean uploadFile(String bucketName, MultipartFile file) {
        if (!bucketExists(bucketName)) {
            logger.error("S3 bucket does not exist: {}", bucketName);
            return false;
        }

        ObjectListing objectListing = listFiles(bucketName);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        int numFiles = objectSummaries.size();

        if (numFiles >= 10) {
            logger.warn("S3 bucket is full ({} files): {}", numFiles, bucketName);
            return false;
        }

        try {
            String fileName = file.getOriginalFilename();
            byte[] bytes = file.getBytes();
            int fileSizeKB = bytes.length / 1024; // Get file size in KB
            if (fileSizeKB > 500) { // Check if file size is greater than 500KB
                logger.warn("File size is too large: {}KB (max allowed: 500KB)", fileSizeKB);
                return false;
            }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            amazonS3.putObject(bucketName, fileName, new ByteArrayInputStream(bytes), metadata);
            logger.info("Uploaded file {} to S3 bucket {}", fileName, bucketName);
            return true;
        } catch (Exception e) {
            logger.error("Error uploading file {} to S3 bucket {}: {}, bucket at capacity", file.getName(), bucketName, e.getMessage());
            return false;
        }
    }

    public void saveFileToDb(FileEntry fileEntry) {
        dynamoDBMapper.save(fileEntry);
    }


    public FileEntry getFileEntry(String username, String fileKey) {
        FileEntry fileEntry = null;
        try {
            fileEntry = dynamoDBMapper.load(FileEntry.class, username, fileKey);
            logger.info("Retrieved file entry for user {} with file key {}", username, fileKey);
        } catch (Exception e) {
            logger.error("Error retrieving file entry for user {} with file key {}: {}", username, fileKey, e.getMessage());
        }
        return fileEntry;
    }
}
