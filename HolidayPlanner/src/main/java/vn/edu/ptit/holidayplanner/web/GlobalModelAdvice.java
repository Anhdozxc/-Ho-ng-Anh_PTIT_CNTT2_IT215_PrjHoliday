package vn.edu.ptit.holidayplanner.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.ptit.holidayplanner.dto.ProfileResponse;
import vn.edu.ptit.holidayplanner.service.UserService;

@ControllerAdvice(basePackages = "vn.edu.ptit.holidayplanner.web")
public class GlobalModelAdvice {
    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public ProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return ProfileResponse.from(userService.requireByEmail(authentication.getName()));
    }
}
