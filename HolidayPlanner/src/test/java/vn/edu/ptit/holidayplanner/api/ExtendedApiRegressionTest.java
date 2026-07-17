package vn.edu.ptit.holidayplanner.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExtendedApiRegressionTest {
    private static final String USER_EMAIL = "user@holidayplanner.vn";
    private static final String USER_PASSWORD = "user1234";
    private static final String ADMIN_EMAIL = "admin@holidayplanner.vn";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private UserService userService;

    @Test
    void tripListReturnsStablePageMetadataAndAppliesSearchStatusAndSort() throws Exception {
        createTrip(USER_EMAIL, USER_PASSWORD, "Regression Alpine Alpha", "3500.00", "PLANNED");
        createTrip(USER_EMAIL, USER_PASSWORD, "Regression Alpine Beta", "4500.00", "PLANNED");
        createTrip(USER_EMAIL, USER_PASSWORD, "Regression Coast Gamma", "5500.00", "DRAFT");

        mockMvc.perform(get("/api/trips")
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .param("q", "regression alpine")
                        .param("status", "PLANNED")
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Regression Alpine Alpha"))
                .andExpect(jsonPath("$.content[0].owner.password").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/trips")
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .param("q", "REGRESSION ALPINE")
                        .param("status", "PLANNED")
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Regression Alpine Beta"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/trips")
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .param("q", "regression value that cannot match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void everyTripChildSupportsCreateReadUpdateAndDeleteAndDetailCalculatesTotalAgainstBudget() throws Exception {
        long tripId = createTrip(USER_EMAIL, USER_PASSWORD,
                "Regression complete child CRUD", "2000.00", "PLANNED");

        long itineraryId = idFrom(postJson(
                "/api/trips/{id}/itinerary", tripId,
                Map.of(
                        "dayNo", 1,
                        "fromTime", "09:00",
                        "toTime", "10:00",
                        "activity", "Morning walk",
                        "location", "Old town",
                        "note", "Initial itinerary"),
                USER_EMAIL, USER_PASSWORD, 201));

        mockMvc.perform(put("/api/trips/{id}/itinerary/{itemId}", tripId, itineraryId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "dayNo", 2,
                                "fromTime", "10:00",
                                "toTime", "11:30",
                                "activity", "Museum visit",
                                "location", "City museum",
                                "note", "Updated itinerary"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itineraryId))
                .andExpect(jsonPath("$.dayNo").value(2))
                .andExpect(jsonPath("$.activity").value("Museum visit"));

        long expenseId = idFrom(postJson(
                "/api/trips/{id}/expenses", tripId,
                Map.of(
                        "category", "FOOD",
                        "amount", 1000.25,
                        "spentDate", "2027-02-10",
                        "note", "Lunch"),
                USER_EMAIL, USER_PASSWORD, 201));

        mockMvc.perform(put("/api/trips/{id}/expenses/{expenseId}", tripId, expenseId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category", "ACCOMMODATION",
                                "amount", 1750.50,
                                "spentDate", "2027-02-11",
                                "note", "One night"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId))
                .andExpect(jsonPath("$.category").value("ACCOMMODATION"))
                .andExpect(jsonPath("$.amount").value(1750.50));

        long checklistId = idFrom(postJson(
                "/api/trips/{id}/checklist", tripId,
                Map.of(
                        "title", "Pack passport",
                        "category", "Documents",
                        "dueDate", "2027-02-09"),
                USER_EMAIL, USER_PASSWORD, 201));

        mockMvc.perform(put("/api/trips/{id}/checklist/{itemId}", tripId, checklistId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Pack passport and tickets",
                                "category", "Travel documents",
                                "dueDate", "2027-02-08"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pack passport and tickets"))
                .andExpect(jsonPath("$.done").value(false));

        setChecklistState(tripId, checklistId, true);
        setChecklistState(tripId, checklistId, true);
        setChecklistState(tripId, checklistId, false);
        setChecklistState(tripId, checklistId, false);

        long bookingId = idFrom(postJson(
                "/api/trips/{id}/bookings", tripId,
                Map.of(
                        "type", "HOTEL",
                        "provider", "Regression Hotel",
                        "bookingCode", "REG-H-001",
                        "price", 900.00,
                        "note", "Refundable"),
                USER_EMAIL, USER_PASSWORD, 201));

        mockMvc.perform(put("/api/trips/{id}/bookings/{bookingId}", tripId, bookingId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "TOUR",
                                "provider", "Regression Tours",
                                "bookingCode", "REG-T-002",
                                "price", 950.00,
                                "note", "Updated booking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.type").value("TOUR"))
                .andExpect(jsonPath("$.provider").value("Regression Tours"));

        mockMvc.perform(get("/api/trips/{id}", tripId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(2000.00))
                .andExpect(jsonPath("$.totalExpense").value(1750.50))
                .andExpect(jsonPath("$.itinerary.length()").value(1))
                .andExpect(jsonPath("$.itinerary[0].id").value(itineraryId))
                .andExpect(jsonPath("$.expenses.length()").value(1))
                .andExpect(jsonPath("$.expenses[0].id").value(expenseId))
                .andExpect(jsonPath("$.checklist.length()").value(1))
                .andExpect(jsonPath("$.checklist[0].id").value(checklistId))
                .andExpect(jsonPath("$.checklist[0].done").value(false))
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].id").value(bookingId));

        deleteChild("/api/trips/{id}/itinerary/{childId}", tripId, itineraryId);
        deleteChild("/api/trips/{id}/expenses/{childId}", tripId, expenseId);
        deleteChild("/api/trips/{id}/checklist/{childId}", tripId, checklistId);
        deleteChild("/api/trips/{id}/bookings/{childId}", tripId, bookingId);

        mockMvc.perform(get("/api/trips/{id}", tripId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0.00))
                .andExpect(jsonPath("$.itinerary").isEmpty())
                .andExpect(jsonPath("$.expenses").isEmpty())
                .andExpect(jsonPath("$.checklist").isEmpty())
                .andExpect(jsonPath("$.bookings").isEmpty());
    }

    @Test
    void checklistCreateDefaultsToPendingWhenDoneIsOmitted() throws Exception {
        long tripId = createTrip(USER_EMAIL, USER_PASSWORD,
                "Regression checklist default", "1000.00", "PLANNED");

        String response = postJson(
                "/api/trips/{id}/checklist", tripId,
                Map.of("title", "Created without state"),
                USER_EMAIL, USER_PASSWORD, 201);

        JsonNode checklist = objectMapper.readTree(response);
        if (!checklist.has("done")) {
            throw new AssertionError("Checklist response must expose its persisted done state: " + response);
        }
        org.assertj.core.api.Assertions.assertThat(checklist.get("done").asBoolean()).isFalse();
    }

    @Test
    void overlappingItineraryReturnsStructuredConflict() throws Exception {
        long tripId = createTrip(USER_EMAIL, USER_PASSWORD,
                "Regression itinerary conflict", "1000.00", "PLANNED");
        postJson(
                "/api/trips/{id}/itinerary", tripId,
                Map.of(
                        "dayNo", 1,
                        "fromTime", "09:00",
                        "toTime", "10:30",
                        "activity", "First activity"),
                USER_EMAIL, USER_PASSWORD, 201);

        mockMvc.perform(post("/api/trips/{id}/itinerary", tripId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "dayNo", 1,
                                "fromTime", "10:00",
                                "toTime", "11:00",
                                "activity", "Overlapping activity"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void ownershipAndParentBindingRejectCrossOwnerCrossTripAndUnknownResources() throws Exception {
        String secondEmail = "extended-owner@example.com";
        String secondPassword = "second123";
        userService.createSeedUser("Extended Owner", secondEmail, secondPassword, Role.USER);

        long ownerTripId = createTrip(USER_EMAIL, USER_PASSWORD,
                "Regression owner trip", "3000.00", "PLANNED");
        long otherOwnerTripId = createTrip(secondEmail, secondPassword,
                "Regression second owner trip", "3200.00", "PLANNED");
        long ownerItineraryId = idFrom(postJson(
                "/api/trips/{id}/itinerary", ownerTripId,
                Map.of(
                        "dayNo", 1,
                        "fromTime", "08:00",
                        "toTime", "09:00",
                        "activity", "Owner-only activity"),
                USER_EMAIL, USER_PASSWORD, 201));

        mockMvc.perform(get("/api/trips/{id}", ownerTripId)
                        .with(asUser(secondEmail, secondPassword)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(post("/api/trips/{id}/expenses", ownerTripId)
                        .with(asUser(secondEmail, secondPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category", "FOOD",
                                "amount", 100,
                                "spentDate", "2027-02-10"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/trips/{id}/itinerary/{itemId}", otherOwnerTripId, ownerItineraryId)
                        .with(asUser(secondEmail, secondPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "dayNo", 1,
                                "fromTime", "10:00",
                                "toTime", "11:00",
                                "activity", "Must not be reassigned"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/trips/{id}", Long.MAX_VALUE)
                        .with(asUser(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void invalidTripAndEveryChildPayloadReturnStructuredFieldErrors() throws Exception {
        long tripId = createTrip(USER_EMAIL, USER_PASSWORD,
                "Regression validation trip", "3000.00", "PLANNED");
        long checklistId = idFrom(postJson(
                "/api/trips/{id}/checklist", tripId,
                Map.of("title", "Valid checklist item"),
                USER_EMAIL, USER_PASSWORD, 201));

        Map<String, Object> invalidTrip = new LinkedHashMap<>();
        invalidTrip.put("title", "Invalid date range");
        invalidTrip.put("destinationId", destinationService.activeDestinations().get(0).getId());
        invalidTrip.put("startDate", "2027-03-15");
        invalidTrip.put("endDate", "2027-03-10");
        invalidTrip.put("peopleCount", 2);
        invalidTrip.put("budget", 1000);
        invalidTrip.put("status", "PLANNED");
        expectValidation(post("/api/trips"), invalidTrip, "dateRangeValid");

        expectValidation(post("/api/trips/{id}/itinerary", tripId), Map.of(
                "dayNo", 1,
                "fromTime", "09:00",
                "activity", "Missing end time"), "timeRangeComplete");

        expectValidation(post("/api/trips/{id}/expenses", tripId), Map.of(
                "category", "FOOD",
                "amount", -0.01,
                "spentDate", "2027-03-10"), "amount");

        expectValidation(post("/api/trips/{id}/checklist", tripId), Map.of(
                "title", "   ",
                "category", "Documents"), "title");

        expectValidation(patch("/api/trips/{id}/checklist/{itemId}/toggle", tripId, checklistId),
                Map.of(), "done");

        expectValidation(post("/api/trips/{id}/bookings", tripId), Map.of(
                "type", "HOTEL",
                "provider", "   ",
                "price", 100), "provider");
    }

    @Test
    void adminDestinationSearchIsPagedAndUserStatusPatchIsDesiredStateAndIdempotent() throws Exception {
        String destinationBody = mockMvc.perform(post("/api/admin/destinations")
                        .with(asUser(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Regression Search Island",
                                "city", "Regression City",
                                "country", "Regression Country",
                                "description", "Unique destination for server-side search",
                                "imageUrl", "/images/destinations/ha-noi.svg",
                                "active", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Regression Search Island"))
                .andReturn().getResponse().getContentAsString();
        long destinationId = idFrom(destinationBody);

        mockMvc.perform(get("/api/admin/destinations")
                        .with(asUser(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .param("q", "regression search")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(destinationId))
                .andExpect(jsonPath("$.content[0].name").value("Regression Search Island"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/admin/destinations")
                        .with(asUser(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        String targetEmail = "extended-status@example.com";
        long targetId = userService.createSeedUser(
                "Extended Status User", targetEmail, "status123", Role.USER).getId();

        setUserStatus(targetId, "LOCKED");
        setUserStatus(targetId, "LOCKED");

        mockMvc.perform(get("/api/admin/users")
                        .with(asUser(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .param("q", targetEmail)
                        .param("role", "USER")
                        .param("status", "LOCKED")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(targetId))
                .andExpect(jsonPath("$.content[0].status").value("LOCKED"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));

        long adminId = userService.requireByEmail(ADMIN_EMAIL).getId();
        mockMvc.perform(patch("/api/admin/users/{id}/toggle", adminId)
                        .with(asUser(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private long createTrip(String email, String password, String title, String budget, String tripStatus)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("destinationId", destinationService.activeDestinations().get(0).getId());
        payload.put("startDate", "2027-02-10");
        payload.put("endDate", "2027-02-12");
        payload.put("peopleCount", 2);
        payload.put("budget", budget);
        payload.put("status", tripStatus);
        payload.put("notes", "Created by ExtendedApiRegressionTest");
        String response = mockMvc.perform(post("/api/trips")
                        .with(asUser(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn().getResponse().getContentAsString();
        return idFrom(response);
    }

    private String postJson(String path, long tripId, Object payload,
                            String email, String password, int expectedStatus) throws Exception {
        return mockMvc.perform(post(path, tripId)
                        .with(asUser(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private void setChecklistState(long tripId, long checklistId, boolean done) throws Exception {
        mockMvc.perform(patch("/api/trips/{id}/checklist/{itemId}/toggle", tripId, checklistId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("done", done))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checklistId))
                .andExpect(jsonPath("$.done").value(done));
    }

    private void deleteChild(String path, long tripId, long childId) throws Exception {
        mockMvc.perform(delete(path, tripId, childId)
                        .with(asUser(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private void expectValidation(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                  Object payload, String field) throws Exception {
        mockMvc.perform(request
                        .with(asUser(USER_EMAIL, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors." + field).isNotEmpty());
    }

    private void setUserStatus(long userId, String desiredStatus) throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/toggle", userId)
                        .with(asUser(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", desiredStatus))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.status").value(desiredStatus));
    }

    private long idFrom(String json) throws Exception {
        JsonNode id = objectMapper.readTree(json).get("id");
        if (id == null || !id.canConvertToLong()) {
            throw new AssertionError("Response does not contain a numeric id: " + json);
        }
        return id.longValue();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private RequestPostProcessor asUser(String email, String password) {
        return SecurityMockMvcRequestPostProcessors.httpBasic(email, password);
    }
}
