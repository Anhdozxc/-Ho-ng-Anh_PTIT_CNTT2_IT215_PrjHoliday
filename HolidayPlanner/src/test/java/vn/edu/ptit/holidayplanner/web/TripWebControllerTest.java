package vn.edu.ptit.holidayplanner.web;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.dto.BookingNoteRequest;
import vn.edu.ptit.holidayplanner.dto.ChecklistRequest;
import vn.edu.ptit.holidayplanner.dto.ExpenseRequest;
import vn.edu.ptit.holidayplanner.dto.ItineraryRequest;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.TripDetailService;
import vn.edu.ptit.holidayplanner.service.TripPlanService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripWebControllerTest {
    private TripPlanService tripPlanService;
    private TripDetailService detailService;
    private TripWebController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        tripPlanService = mock(TripPlanService.class);
        detailService = mock(TripDetailService.class);
        controller = new TripWebController(tripPlanService, detailService, mock(DestinationService.class));
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@holidayplanner.vn");
    }

    @Test
    void listUsesBoundedDatabasePage() {
        when(tripPlanService.searchVisible(
                "user@holidayplanner.vn", "Đà Nẵng", null, "title", "asc", 1, 12))
                .thenReturn(new PageImpl<>(List.of()));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.list("Đà Nẵng", null, "title", "asc", 1, 12, authentication, model))
                .isEqualTo("trips/list");

        verify(tripPlanService).searchVisible(
                "user@holidayplanner.vn", "Đà Nẵng", null, "title", "asc", 1, 12);
        assertThat(model).containsKeys("tripPage", "trips", "selectedSize");
    }

    @Test
    void detailValidationPreservesSubmittedFormAndBindingErrors() {
        ItineraryRequest request = new ItineraryRequest();
        request.setDayNo(1);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "itineraryRequest");
        result.rejectValue("activity", "required", "Hoạt động không được để trống");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.addItinerary(7L, request, result, authentication, redirect);

        assertThat(view).isEqualTo("redirect:/trips/7");
        assertThat(redirect.getFlashAttributes().get("itineraryRequest")).isSameAs(request);
        assertThat(redirect.getFlashAttributes().get("activeTripTab")).isEqualTo("itinerary");
        assertThat(redirect.getFlashAttributes())
                .containsKey(BindingResult.MODEL_KEY_PREFIX + "itineraryRequest");
    }

    @Test
    void invalidItineraryUpdateKeepsEditingModeAndItineraryTab() {
        ItineraryRequest request = new ItineraryRequest();
        request.setDayNo(1);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "itineraryRequest");
        result.rejectValue("activity", "required", "Hoạt động không được để trống");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateItinerary(7L, 41L, request, result, authentication, redirect);

        assertUpdateValidation(view, redirect, request, result,
                "itineraryRequest", "editingItineraryId", 41L, "itinerary");
    }

    @Test
    void invalidExpenseUpdateKeepsEditingModeAndExpensesTab() {
        ExpenseRequest request = new ExpenseRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "expenseRequest");
        result.rejectValue("amount", "required", "Số tiền là bắt buộc");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateExpense(
                7L, 42L, request, result, ExpenseCategory.FOOD, authentication, redirect);

        assertUpdateValidation(view, redirect, request, result,
                "expenseRequest", "editingExpenseId", 42L, "expenses");
        assertThat(redirect.getAttribute("tab")).isEqualTo("expenses");
        assertThat(redirect.getAttribute("expenseCategory")).isEqualTo("FOOD");
    }

    @Test
    void invalidChecklistUpdateKeepsEditingModeAndChecklistTab() {
        ChecklistRequest request = new ChecklistRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "checklistRequest");
        result.rejectValue("title", "required", "Tên việc không được để trống");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateChecklist(7L, 43L, request, result, authentication, redirect);

        assertUpdateValidation(view, redirect, request, result,
                "checklistRequest", "editingChecklistId", 43L, "checklist");
    }

    @Test
    void invalidBookingUpdateKeepsEditingModeAndBookingsTab() {
        BookingNoteRequest request = new BookingNoteRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "bookingRequest");
        result.rejectValue("provider", "required", "Nhà cung cấp không được để trống");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateBooking(7L, 44L, request, result, authentication, redirect);

        assertUpdateValidation(view, redirect, request, result,
                "bookingRequest", "editingBookingId", 44L, "bookings");
    }

    @Test
    void businessErrorDuringUpdateAlsoKeepsEditingModeInsteadOfFallingBackToCreate() {
        BookingNoteRequest request = new BookingNoteRequest();
        request.setType(BookingType.HOTEL);
        request.setProvider("Khách sạn thử nghiệm");
        request.setPrice(new BigDecimal("1200000"));
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "bookingRequest");
        doThrow(new IllegalArgumentException("Booking không thuộc chuyến đi này"))
                .when(detailService).updateBooking(7L, 44L, request, "user@holidayplanner.vn");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateBooking(7L, 44L, request, result, authentication, redirect);

        assertUpdateValidation(view, redirect, request, result,
                "bookingRequest", "editingBookingId", 44L, "bookings");
        assertThat(result.getGlobalError()).isNotNull();
        assertThat(result.getGlobalError().getDefaultMessage())
                .isEqualTo("Booking không thuộc chuyến đi này");
        verify(detailService).updateBooking(7L, 44L, request, "user@holidayplanner.vn");
    }

    @Test
    void notFoundFromChildActionIsNotSwallowedAsFlashMessage() {
        doThrow(new EntityNotFoundException("Không tìm thấy khoản chi"))
                .when(detailService).deleteExpense(9L, 99L, "user@holidayplanner.vn");

        assertThatThrownBy(() -> controller.deleteExpense(
                9L, 99L, authentication, new RedirectAttributesModelMap()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Không tìm thấy khoản chi");
    }

    @Test
    void ordinaryBusinessValidationStillReturnsToDetailWithFeedback() {
        doThrow(new IllegalArgumentException("Số tiền không được âm"))
                .when(detailService).deleteExpense(9L, 99L, "user@holidayplanner.vn");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertThat(controller.deleteExpense(9L, 99L, authentication, redirect))
                .isEqualTo("redirect:/trips/9");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("Số tiền không được âm");
    }

    private void assertUpdateValidation(String view, RedirectAttributesModelMap redirect,
                                        Object request, BindingResult result, String formName,
                                        String editingIdName, Long editingId, String tab) {
        assertThat(view).isEqualTo("redirect:/trips/7");
        assertThat(redirect.getFlashAttributes().get(formName)).isSameAs(request);
        assertThat(redirect.getFlashAttributes().get(editingIdName)).isEqualTo(editingId);
        assertThat(redirect.getFlashAttributes().get("activeTripTab")).isEqualTo(tab);
        assertThat(redirect.getFlashAttributes().get(BindingResult.MODEL_KEY_PREFIX + formName))
                .isSameAs(result);
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripDetailUpdateModeTemplateTest {
    private static final String OWNER_EMAIL = "user@holidayplanner.vn";

    @Autowired private MockMvc mockMvc;
    @Autowired private TripPlanRepository tripPlanRepository;
    @Autowired private TripDetailService tripDetailService;

    @Test
    void invalidUpdatesRenderAllFourSidebarsInUpdateMode() throws Exception {
        var trip = tripPlanRepository.findAll().stream()
                .filter(candidate -> OWNER_EMAIL.equalsIgnoreCase(candidate.getOwner().getEmail()))
                .findFirst()
                .orElseThrow();
        long tripId = trip.getId();

        long itineraryId = tripDetailService.itinerary(trip).get(0).getId();
        assertInvalidUpdateEditor(tripId, itineraryId,
                "/trips/{tripId}/itinerary/{childId}/update",
                "editingItineraryId", "itinerary", "itinerary",
                "/trips/" + tripId + "/itinerary/" + itineraryId + "/update",
                "/trips/" + tripId + "/itinerary",
                "dayNo", "1", "activity", " ");

        long expenseId = tripDetailService.expenses(trip).get(0).getId();
        assertInvalidUpdateEditor(tripId, expenseId,
                "/trips/{tripId}/expenses/{childId}/update",
                "editingExpenseId", "expenses", "expense",
                "/trips/" + tripId + "/expenses/" + expenseId + "/update",
                "/trips/" + tripId + "/expenses",
                "category", ExpenseCategory.FOOD.name(), "amount", "-1",
                "expenseCategory", ExpenseCategory.FOOD.name());

        long checklistId = tripDetailService.checklist(trip).get(0).getId();
        assertInvalidUpdateEditor(tripId, checklistId,
                "/trips/{tripId}/checklist/{childId}/update",
                "editingChecklistId", "checklist", "checklist",
                "/trips/" + tripId + "/checklist/" + checklistId + "/update",
                "/trips/" + tripId + "/checklist",
                "title", " ", "done", "false");

        long bookingId = tripDetailService.bookings(trip).get(0).getId();
        assertInvalidUpdateEditor(tripId, bookingId,
                "/trips/{tripId}/bookings/{childId}/update",
                "editingBookingId", "bookings", "booking",
                "/trips/" + tripId + "/bookings/" + bookingId + "/update",
                "/trips/" + tripId + "/bookings",
                "type", BookingType.HOTEL.name(), "provider", " ", "price", "100000");
    }

    @Test
    void globalBusinessErrorIsRenderedInsideUpdateSidebar() throws Exception {
        var trip = tripPlanRepository.findAll().stream()
                .filter(candidate -> OWNER_EMAIL.equalsIgnoreCase(candidate.getOwner().getEmail()))
                .findFirst()
                .orElseThrow();
        long bookingId = tripDetailService.bookings(trip).get(0).getId();
        BookingNoteRequest request = new BookingNoteRequest();
        request.setType(BookingType.HOTEL);
        request.setProvider("Nhà cung cấp đang sửa");
        request.setPrice(new BigDecimal("100000"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "bookingRequest");
        bindingResult.reject("business", "Business editor error");

        mockMvc.perform(get("/trips/{id}", trip.getId())
                        .with(user(OWNER_EMAIL).roles("USER"))
                        .flashAttr("bookingRequest", request)
                        .flashAttr(BindingResult.MODEL_KEY_PREFIX + "bookingRequest", bindingResult)
                        .flashAttr("editingBookingId", bookingId)
                        .flashAttr("activeTripTab", "bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Business editor error")))
                .andExpect(content().string(containsString("data-child-form=\"booking\"")))
                .andExpect(content().string(containsString("data-editing-id=\"" + bookingId + "\"")));
    }

    private void assertInvalidUpdateEditor(long tripId, long childId, String endpoint,
                                           String editingIdAttribute, String tab, String formMarker,
                                           String expectedUpdateAction, String forbiddenCreateAction,
                                           String... parameters) throws Exception {
        MockHttpServletRequestBuilder request = post(endpoint, tripId, childId)
                .characterEncoding("UTF-8")
                .with(user(OWNER_EMAIL).roles("USER"))
                .with(csrf());
        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }

        MvcResult invalidUpdate = mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(editingIdAttribute, childId))
                .andExpect(flash().attribute("activeTripTab", tab))
                .andReturn();

        var renderedEditor = mockMvc.perform(get(invalidUpdate.getResponse().getRedirectedUrl())
                        .with(user(OWNER_EMAIL).roles("USER"))
                        .flashAttrs(invalidUpdate.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-child-form=\"" + formMarker + "\"")))
                .andExpect(content().string(containsString("data-editing-id=\"" + childId + "\"")))
                .andExpect(content().string(containsString("action=\"" + expectedUpdateAction + "\"")))
                .andExpect(content().string(not(containsString("action=\"" + forbiddenCreateAction + "\""))));
        if ("expense".equals(formMarker)) {
            renderedEditor.andExpect(content().string(containsString("name=\"tab\" value=\"expenses\"")))
                    .andExpect(content().string(containsString("name=\"expenseCategory\" value=\"FOOD\"")));
        }
    }
}
