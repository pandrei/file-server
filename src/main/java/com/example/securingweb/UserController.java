package com.example.securingweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;

@Controller
public class UserController {
    private final userStorageService userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(userStorageService userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User(null, "", "", "", "", "", ""));
        return "register";
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