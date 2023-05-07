package com.example.securingweb.controller;

import com.example.securingweb.entity.FileEntry;
import com.example.securingweb.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Controller
public class FileController {
    @Autowired
    private FileStorageService fileStorageService;

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @PostMapping("/createBucket")
    public String createBucket(Authentication authentication) {
        String bucketName = authentication.getName() + "-files";
        if (fileStorageService.createBucket(bucketName)) {
            logger.info("Bucket created for user {}", authentication.getName());
        } else {
            logger.warn("Bucket already exists for user {}", authentication.getName());
        }
        return "redirect:/allocatingResources";
    }

    @GetMapping("/allocatingResources")
    public String showAllocatingResources(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String bucketName = username + "-files";
        logger.info("Bucket name is {} ", bucketName);
        Boolean bucketExists = fileStorageService.bucketExists(bucketName);
        model.addAttribute("bucketExists", fileStorageService.bucketExists(bucketName));

        if(bucketExists) {
            fileStorageService.startLambdaToTrackS3BucketEvents(bucketName);
        }
        return "allocatingResources";
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String bucketName = username + "-files";

        // Check if bucket exists
        boolean bucketExists = fileStorageService.bucketExists(bucketName);
        if (bucketExists) {
            // If bucket exists, upload the file
            fileStorageService.uploadFile(bucketName, file);
            String fileName = file.getOriginalFilename();
            FileEntry fileEntry = new FileEntry(1L, fileName, username, new Date(), FileEntry.Status.NOT_STARTED);
            fileStorageService.saveFileToDb(fileEntry);
        }

        return "redirect:/home";
    }
}
