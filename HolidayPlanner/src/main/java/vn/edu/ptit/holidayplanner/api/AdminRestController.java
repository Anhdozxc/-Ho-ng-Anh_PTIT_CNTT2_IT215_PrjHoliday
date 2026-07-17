package vn.edu.ptit.holidayplanner.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.holidayplanner.dto.DestinationRequest;
import vn.edu.ptit.holidayplanner.dto.DestinationStatusRequest;
import vn.edu.ptit.holidayplanner.dto.UserStatusRequest;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
    private final DestinationService destinationService;
    private final UserService userService;

    public AdminRestController(DestinationService destinationService, UserService userService) {
        this.destinationService = destinationService;
        this.userService = userService;
    }

    @GetMapping("/destinations")
    public PageResponse<Map<String,Object>> destinations(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(destinationService.search(q, active, page, size), ApiMapper::destination);
    }

    @PostMapping("/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createDestination(@Valid @RequestBody DestinationRequest request) {
        return ApiMapper.destination(destinationService.create(request));
    }

    @PutMapping("/destinations/{id}")
    public Map<String,Object> updateDestination(@PathVariable Long id, @Valid @RequestBody DestinationRequest request) {
        return ApiMapper.destination(destinationService.update(id, request));
    }

    @DeleteMapping("/destinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDestination(@PathVariable Long id) {
        destinationService.deactivate(id);
    }

    @PatchMapping("/destinations/{id}/status")
    public Map<String,Object> setDestinationStatus(@PathVariable Long id,
                                                    @Valid @RequestBody DestinationStatusRequest request) {
        return ApiMapper.destination(destinationService.setActive(id, request.getActive()));
    }

    @PostMapping(value = "/destinations/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> uploadDestinationImage(@PathVariable Long id,
                                                     @RequestPart("image") MultipartFile image) {
        return ApiMapper.destination(destinationService.uploadImage(id, image));
    }

    @DeleteMapping("/destinations/{id}/image")
    public Map<String,Object> deleteDestinationImage(@PathVariable Long id) {
        return ApiMapper.destination(destinationService.deleteImage(id));
    }

    @GetMapping("/users")
    public PageResponse<Map<String,Object>> users(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Role role,
                                                  @RequestParam(required = false) UserStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Page<UserAccount> result = userService.search(q, role, status, page, size);
        return PageResponse.from(result, ApiMapper::user);
    }

    @PatchMapping({"/users/{id}/status", "/users/{id}/toggle"})
    public Map<String,Object> setUserStatus(@PathVariable Long id,
                                           @Valid @RequestBody UserStatusRequest request,
                                           Authentication authentication) {
        return ApiMapper.user(userService.setStatus(id, request.getStatus(), authentication.getName()));
    }
}
