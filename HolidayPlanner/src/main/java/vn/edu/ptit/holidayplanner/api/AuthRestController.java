package vn.edu.ptit.holidayplanner.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.holidayplanner.dto.RegisterRequest;
import vn.edu.ptit.holidayplanner.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UserService userService;

    public AuthRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@Valid @RequestBody RegisterRequest request) {
        return ApiMapper.user(userService.register(request));
    }
}
