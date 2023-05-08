package com.example.securingweb.controller;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.securingweb.entity.FileEntry;
import com.example.securingweb.entity.FileForm;
import com.example.securingweb.entity.Relationship;
import com.example.securingweb.entity.User;
import com.example.securingweb.service.FileStorageService;
import com.example.securingweb.service.RelationshipService;
import com.example.securingweb.service.userStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class UserController {
    private final userStorageService userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(userStorageService userRepository) {
        this.userRepository = userRepository;
    }


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User(null, "", "", "", "", "", ""));
        return "register";
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        if (request.getUserPrincipal() != null) {
            return "redirect:/home";
        }
        model.addAttribute("error", request.getParameter("error") != null);
        model.addAttribute("logout", request.getParameter("logout") != null);
        model.addAttribute("registrationSuccess", request.getParameter("registrationSuccess") != null);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return "redirect:/home";
        } catch (AuthenticationException e) {
            request.setAttribute("error", true);
            return "login";
        }
    }

    @Secured("ROLE_USER")
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

            List<FileEntry> fileEntries = new ArrayList<>();
            for (S3ObjectSummary file : files.getObjectSummaries()) {
                // Get the file key (i.e., object key)
                String fileKey = file.getKey();
                // Get the corresponding file entry from DynamoDB
                FileEntry fileEntry = fileStorageService.getFileEntry(username, fileKey);

                if (fileEntry != null) {
                    // Add the file entry to the list
                    fileEntries.add(fileEntry);
                }
            }

            // Build a map of file relations
            Map<String, ArrayList<String>> fileRelations = new HashMap<>();
            List<Relationship> relationships = relationshipService.getRelationships();
            for (FileEntry fileEntry : fileEntries) {
                String fileKey = fileEntry.getFileName();
                String left = fileKey.substring(0, fileKey.lastIndexOf(".txt"));
                ArrayList<String> relatedFiles = new ArrayList<>();
                for (Relationship relationship : relationships) {
                    String right = relationship.getRight();
                    if (left.equals(relationship.getLeft())) {
                        relatedFiles.add(right);
                    }
                }
                fileRelations.put(left, relatedFiles);
            }
            logger.info("File relations is {}", fileRelations);
            model.addAttribute("fileRelations", fileRelations);
            // Add the file entries to the model
            model.addAttribute("fileEntries", fileEntries);

            // Add form for file upload
            model.addAttribute("fileForm", new FileForm());
        }

        return "home";
    }

    @PostMapping("/register")
    public String processRegistrationForm(@ModelAttribute("user") User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is already authenticated with a registered account
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            logger.info("User is already authenticated with registered account. Redirecting to login page.");
            return "redirect:/login";
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("registrationSuccess", true);
        return "redirect:/login?registrationSuccess";
    }
}