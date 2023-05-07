package com.example.securingweb;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UserController {
    private final userStorageService userRepository;

    @Autowired
    private FileStorageService fileStorageService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(userStorageService userRepository) {
        this.userRepository = userRepository;
    }


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User(null, "", "", "", "", "", ""));
        return "register";
    }

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
        }

        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String bucketName = username + "-files";

        // Check if bucket exists
        boolean bucketExists = fileStorageService.bucketExists(bucketName);
        model.addAttribute("bucketExists", bucketExists);

        if (bucketExists) {
            // If bucket exists, list the files
            ObjectListing files = fileStorageService.listFiles(bucketName);
            model.addAttribute("bucketName", bucketName);
            model.addAttribute("files", files.getObjectSummaries());

            // Add form for file upload
            model.addAttribute("fileForm", new FileForm());
        }

        return "home";
    }

    @PostMapping("/register")
    public String processRegistrationForm(@ModelAttribute("user") User user, BindingResult bindingResult) {
        logger.info("controller called");
        if (bindingResult.hasErrors()) {
            return "register";
        }

        userRepository.save(user);

        return "redirect:/login?registrationSuccess";
    }
}