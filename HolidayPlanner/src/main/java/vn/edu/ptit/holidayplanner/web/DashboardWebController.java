package vn.edu.ptit.holidayplanner.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.ptit.holidayplanner.service.DashboardService;

@Controller
public class DashboardWebController {
    private final DashboardService dashboardService;

    public DashboardWebController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("stats", dashboardService.stats(authentication.getName()));
        return "dashboard";
    }
}
