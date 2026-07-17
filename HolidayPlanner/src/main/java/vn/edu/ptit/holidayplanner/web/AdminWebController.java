package vn.edu.ptit.holidayplanner.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.dto.DestinationRequest;
import vn.edu.ptit.holidayplanner.media.ImageStorageException;
import vn.edu.ptit.holidayplanner.media.ImageValidationException;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminWebController {
    private static final int DESTINATION_PAGE_SIZE = 10;

    private final DestinationService destinationService;
    private final UserService userService;

    public AdminWebController(DestinationService destinationService, UserService userService) {
        this.destinationService = destinationService;
        this.userService = userService;
    }

    @GetMapping("/destinations")
    public String destinations(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Boolean active,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        return renderDestinations(q, active, page, model);
    }

    @PostMapping("/destinations")
    public String createDestination(
                                    @Valid @ModelAttribute("destinationRequest") DestinationRequest request,
                                    BindingResult result,
                                    @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(name = "filterActive", required = false) Boolean filterActive,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model,
                                    RedirectAttributes ra) {
        if (result.hasErrors()) {
            return renderDestinations(q, filterActive, page, model);
        }
        try {
            destinationService.create(request, imageFile);
        } catch (ImageValidationException | ImageStorageException exception) {
            result.reject("destination.image", exception.getMessage());
            return renderDestinations(q, filterActive, page, model);
        }
        ra.addFlashAttribute("success", "Đã thêm điểm đến");
        return redirectToDestinations(q, filterActive, page, ra);
    }

    @PostMapping("/destinations/{id}")
    public String updateDestination(@PathVariable Long id,
                                    @Valid @ModelAttribute("editDestinationRequest") DestinationRequest request,
                                    BindingResult result,
                                    @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(name = "filterActive", required = false) Boolean filterActive,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model,
                                    RedirectAttributes ra) {
        if (result.hasErrors()) {
            return renderDestinationEditError(id, request, q, filterActive, page, model);
        }
        try {
            destinationService.update(id, request, imageFile);
        } catch (ImageValidationException | ImageStorageException exception) {
            result.reject("destination.image", exception.getMessage());
            return renderDestinationEditError(id, request, q, filterActive, page, model);
        }
        ra.addFlashAttribute("success", "Đã cập nhật điểm đến");
        return redirectToDestinations(q, filterActive, page, ra);
    }

    @PostMapping("/destinations/{id}/toggle")
    public String setDestinationStatus(@PathVariable Long id,
                                       @RequestParam boolean active,
                                       RedirectAttributes ra) {
        destinationService.setActive(id, active);
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái điểm đến");
        return "redirect:/admin/destinations";
    }

    @PostMapping("/destinations/{id}/image")
    public String uploadDestinationImage(@PathVariable Long id,
                                         @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                                         RedirectAttributes ra) {
        try {
            destinationService.uploadImage(id, imageFile);
            ra.addFlashAttribute("success", "Đã cập nhật ảnh điểm đến");
        } catch (ImageValidationException | ImageStorageException exception) {
            ra.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/destinations";
    }

    @PostMapping("/destinations/{id}/image/delete")
    public String deleteDestinationImage(@PathVariable Long id, RedirectAttributes ra) {
        destinationService.deleteImage(id);
        ra.addFlashAttribute("success", "Đã đưa ảnh điểm đến về ảnh mặc định");
        return "redirect:/admin/destinations";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Role role,
                        @RequestParam(required = false) UserStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var users = userService.search(q, role, status, page, 10);
        model.addAttribute("users", users);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles", Role.values());
        model.addAttribute("statuses", UserStatus.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String setUserStatus(@PathVariable Long id,
                                @RequestParam UserStatus status,
                                Authentication authentication,
                                RedirectAttributes ra) {
        userService.setStatus(id, status, authentication.getName());
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản");
        return "redirect:/admin/users";
    }

    private String renderDestinationEditError(Long id, DestinationRequest request,
                                              String q, Boolean active, int page, Model model) {
        model.addAttribute("editDestinationId", id);
        model.addAttribute("editDestinationRequest", request);
        return renderDestinations(q, active, page, model);
    }

    private String renderDestinations(String q, Boolean active, int page, Model model) {
        model.addAttribute("destinations",
                destinationService.search(q, active, page, DESTINATION_PAGE_SIZE));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("selectedActive", active);
        if (!model.containsAttribute("destinationRequest")) {
            model.addAttribute("destinationRequest", new DestinationRequest());
        }
        model.addAttribute("imageUploadAvailable", destinationService.isImageUploadAvailable());
        return "admin/destinations";
    }

    private String redirectToDestinations(String q, Boolean active, int page,
                                          RedirectAttributes redirectAttributes) {
        if (q != null && !q.isBlank()) {
            redirectAttributes.addAttribute("q", q.trim());
        }
        if (active != null) {
            redirectAttributes.addAttribute("active", active);
        }
        if (page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        return "redirect:/admin/destinations";
    }
}
