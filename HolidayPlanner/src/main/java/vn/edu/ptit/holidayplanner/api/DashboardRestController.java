package vn.edu.ptit.holidayplanner.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.holidayplanner.service.DashboardService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {
    private final DashboardService dashboardService;

    public DashboardRestController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Map<String,Object> stats(Authentication authentication) {
        DashboardService.DashboardStats s = dashboardService.stats(authentication.getName());
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalTrips", s.totalTrips());
        m.put("totalExpense", s.totalExpense());
        m.put("openChecklist", s.openChecklist());
        m.put("adminView", s.adminView());
        m.put("expenseByCategory", s.expenseByCategory());
        m.put("tripsByStatus", s.tripsByStatus());
        m.put("upcomingTrips", s.upcomingTrips().stream().map(ApiMapper::trip).toList());
        return m;
    }
}
