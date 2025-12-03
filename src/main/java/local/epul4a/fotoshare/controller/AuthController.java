package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.UserRegistrationDTO;
import local.epul4a.fotoshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDTO dto,
                               BindingResult result,
                               Model model) {
        if (userService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.email", "Un compte avec cet email existe déjà");
        }

        if (userService.existsByUsername(dto.getUsername())) {
            result.rejectValue("username", "error.username", "Ce nom d'user est déjà pris");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.password", "Les mots de passe ne correspondent pas");
        }

        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(dto);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}

