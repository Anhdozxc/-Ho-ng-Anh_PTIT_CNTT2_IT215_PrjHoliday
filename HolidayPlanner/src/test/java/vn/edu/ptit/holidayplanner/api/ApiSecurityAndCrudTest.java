package vn.edu.ptit.holidayplanner.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.TripPlanService;
import vn.edu.ptit.holidayplanner.service.UserService;
import vn.edu.ptit.holidayplanner.dto.TripPlanRequest;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityAndCrudTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DestinationService destinationService;
    @Autowired private UserService userService;
    @Autowired private TripPlanService tripPlanService;

    @Test
    void unauthenticatedApiReturnsStandardJson401() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void userCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void lockedUserCannotAuthenticate() throws Exception {
        var account = userService.requireByEmail("user@holidayplanner.vn");
        userService.setStatus(account.getId(), vn.edu.ptit.holidayplanner.domain.enums.UserStatus.LOCKED,
                "admin@holidayplanner.vn");

        mockMvc.perform(get("/api/trips")
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validationErrorContainsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void userCannotReadAnotherUsersTrip() throws Exception {
        userService.createSeedUser("Người thứ hai", "second@example.com", "password123", Role.USER);
        var destination = destinationService.activeDestinations().get(0);
        TripPlanRequest request = validTrip(destination.getId());
        var trip = tripPlanService.create(request, "user@holidayplanner.vn");

        mockMvc.perform(get("/api/trips/{id}", trip.getId())
                        .with(httpBasic("second@example.com", "password123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void mainTripAndChildCrudApiFlowWorks() throws Exception {
        Long destinationId = destinationService.activeDestinations().get(0).getId();
        String tripJson = objectMapper.writeValueAsString(Map.of(
                "title", "API smoke trip",
                "destinationId", destinationId,
                "startDate", "2026-11-10",
                "endDate", "2026-11-12",
                "peopleCount", 2,
                "budget", 4000000,
                "status", "PLANNED",
                "notes", "Created by MockMvc"));
        String body = mockMvc.perform(post("/api/trips")
                        .with(httpBasic("user@holidayplanner.vn", "user1234"))
                        .contentType(MediaType.APPLICATION_JSON).content(tripJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode trip = objectMapper.readTree(body);
        long tripId = trip.get("id").asLong();

        String itineraryJson = objectMapper.writeValueAsString(Map.of(
                "dayNo", 1, "fromTime", "09:00", "toTime", "10:00",
                "activity", "Check-in", "location", "Khách sạn"));
        mockMvc.perform(post("/api/trips/{id}/itinerary", tripId)
                        .with(httpBasic("user@holidayplanner.vn", "user1234"))
                        .contentType(MediaType.APPLICATION_JSON).content(itineraryJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activity").value("Check-in"));

        mockMvc.perform(get("/api/trips/{id}", tripId)
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itinerary.length()").value(1))
                .andExpect(jsonPath("$.owner.password").doesNotExist());
    }

    @Test
    void authenticatedTemplatesRenderForUserAndAdmin() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("user@holidayplanner.vn").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"));
        mockMvc.perform(get("/trips").with(user("user@holidayplanner.vn").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("trips/list"));
        mockMvc.perform(get("/profile").with(user("user@holidayplanner.vn").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("profile"));
        mockMvc.perform(get("/admin/users").with(user("admin@holidayplanner.vn").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/users"));
        mockMvc.perform(get("/admin/destinations").with(user("admin@holidayplanner.vn").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/destinations"));
    }

    private TripPlanRequest validTrip(Long destinationId) {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Ownership test");
        request.setDestinationId(destinationId);
        request.setStartDate(LocalDate.of(2026, 11, 10));
        request.setEndDate(LocalDate.of(2026, 11, 12));
        request.setPeopleCount(2);
        request.setBudget(new BigDecimal("4000000"));
        request.setStatus(TripStatus.PLANNED);
        return request;
    }
}
