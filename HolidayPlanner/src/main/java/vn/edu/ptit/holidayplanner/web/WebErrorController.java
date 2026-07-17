package vn.edu.ptit.holidayplanner.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebErrorController {
    @GetMapping("/forbidden")
    public String forbidden() {
        return "error/403";
    }
}
