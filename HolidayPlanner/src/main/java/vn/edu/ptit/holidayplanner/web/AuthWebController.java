package vn.edu.ptit.holidayplanner.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ptit.holidayplanner.dto.RegisterRequest;
import vn.edu.ptit.holidayplanner.service.UserService;

@Controller
public class AuthWebController {
    private final UserService userService;

    public AuthWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(registerRequest);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công. Hãy đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            return "auth/register";
        }
    }
}
