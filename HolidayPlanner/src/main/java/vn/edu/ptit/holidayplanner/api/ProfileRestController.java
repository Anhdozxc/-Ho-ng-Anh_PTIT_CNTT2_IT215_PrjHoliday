package vn.edu.ptit.holidayplanner.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.holidayplanner.dto.ChangePasswordRequest;
import vn.edu.ptit.holidayplanner.dto.ProfileRequest;
import vn.edu.ptit.holidayplanner.dto.ProfileResponse;
import vn.edu.ptit.holidayplanner.service.UserService;

@RestController
@RequestMapping("/api/profile")
public class ProfileRestController {
    private final UserService userService;

    public ProfileRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ProfileResponse profile(Authentication authentication) {
        return ProfileResponse.from(userService.requireByEmail(authentication.getName()));
    }

    @PutMapping
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request,
                                         Authentication authentication) {
        return ProfileResponse.from(userService.updateProfile(authentication.getName(), request));
    }

    @RequestMapping(value = "/password", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication) {
        userService.changePassword(authentication.getName(), request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse updateAvatar(@RequestPart("image") MultipartFile image,
                                        Authentication authentication) {
        return ProfileResponse.from(userService.updateAvatar(authentication.getName(), image));
    }

    @DeleteMapping("/avatar")
    public ProfileResponse deleteAvatar(Authentication authentication) {
        return ProfileResponse.from(userService.deleteAvatar(authentication.getName()));
    }
}
