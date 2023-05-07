package com.example.securingweb;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FileStorageService
{

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final AmazonS3 amazonS3;

    private final AWSLambda lambda;
    public FileStorageService(AmazonS3 amazonS3, AWSLambda lambda) {
        this.amazonS3 = amazonS3;
        this.lambda = lambda;
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

}
