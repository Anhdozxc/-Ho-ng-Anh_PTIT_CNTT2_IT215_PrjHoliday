package vn.edu.ptit.holidayplanner.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ptit.holidayplanner.dto.ChangePasswordRequest;
import vn.edu.ptit.holidayplanner.dto.ProfileRequest;
import vn.edu.ptit.holidayplanner.media.ImageStorageException;
import vn.edu.ptit.holidayplanner.media.ImageValidationException;
import vn.edu.ptit.holidayplanner.service.UserService;

@Controller
public class ProfileWebController {
    private final UserService userService;

    public ProfileWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        var user = userService.requireByEmail(authentication.getName());
        if (!model.containsAttribute("profileRequest")) {
            ProfileRequest profile = new ProfileRequest();
            profile.setFullName(user.getFullName());
            model.addAttribute("profileRequest", profile);
        }
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        model.addAttribute("imageUploadAvailable", userService.isImageUploadAvailable());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute ProfileRequest profileRequest,
                                BindingResult result, Authentication authentication,
                                RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            prepareProfileModel(authentication, model, false);
            return "profile";
        }
        userService.updateProfile(authentication.getName(), profileRequest);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật hồ sơ");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest changePasswordRequest,
                                 BindingResult result, Authentication authentication,
                                 RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            prepareProfileModel(authentication, model, true);
            return "profile";
        }
        try {
            userService.changePassword(authentication.getName(), changePasswordRequest);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException ex) {
            String field = passwordErrorField(ex.getMessage());
            result.rejectValue(field, "invalid", ex.getMessage());
            prepareProfileModel(authentication, model, true);
            return "profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam(name = "image", required = false) MultipartFile image,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.updateAvatar(authentication.getName(), image);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật ảnh đại diện");
        } catch (ImageValidationException | ImageStorageException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/avatar/delete")
    public String deleteAvatar(Authentication authentication, RedirectAttributes redirectAttributes) {
        userService.deleteAvatar(authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Đã xóa ảnh đại diện");
        return "redirect:/profile";
    }

    private void prepareProfileModel(Authentication authentication, Model model, boolean profileFormMissing) {
        var user = userService.requireByEmail(authentication.getName());
        if (profileFormMissing && !model.containsAttribute("profileRequest")) {
            ProfileRequest profile = new ProfileRequest();
            profile.setFullName(user.getFullName());
            model.addAttribute("profileRequest", profile);
        }
        if (!profileFormMissing && !model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        model.addAttribute("imageUploadAvailable", userService.isImageUploadAvailable());
    }

    private String passwordErrorField(String message) {
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("xác nhận")) {
            return "confirmPassword";
        }
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("hiện tại")
                && !message.toLowerCase(java.util.Locale.ROOT).contains("phải khác")) {
            return "currentPassword";
        }
        return "newPassword";
    }
}
