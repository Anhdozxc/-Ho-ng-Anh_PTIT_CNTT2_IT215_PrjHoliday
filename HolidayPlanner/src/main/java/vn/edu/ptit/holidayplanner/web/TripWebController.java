package vn.edu.ptit.holidayplanner.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.dto.*;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.TripDetailService;
import vn.edu.ptit.holidayplanner.service.TripPlanService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vn.edu.ptit.holidayplanner.domain.ChecklistItem;

@Controller
@RequestMapping("/trips")
public class TripWebController {
    private final TripPlanService tripPlanService;
    private final TripDetailService detailService;
    private final DestinationService destinationService;

    public TripWebController(TripPlanService tripPlanService, TripDetailService detailService,
                             DestinationService destinationService) {
        this.tripPlanService = tripPlanService;
        this.detailService = detailService;
        this.destinationService = destinationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) TripStatus status,
                       @RequestParam(defaultValue = "startDate") String sort,
                       @RequestParam(defaultValue = "desc") String direction,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "9") int size,
                       Authentication authentication, Model model) {
        Page<TripPlan> tripPage = tripPlanService.searchVisible(
                authentication.getName(), q, status, sort, direction, page, size);
        model.addAttribute("tripPage", tripPage);
        model.addAttribute("trips", tripPage.getContent());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("selectedDirection", direction);
        model.addAttribute("selectedSize", tripPage.getSize());
        model.addAttribute("statuses", TripStatus.values());
        return "trips/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        prepareForm(model, new TripPlanRequest(), "Tạo kế hoạch mới", "/trips");
        return "trips/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("tripRequest") TripPlanRequest request,
                         BindingResult bindingResult, Authentication authentication, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Tạo kế hoạch mới", "/trips");
            return "trips/form";
        }
        try {
            TripPlan trip = tripPlanService.create(request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Đã tạo kế hoạch chuyến đi");
            return "redirect:/trips/" + trip.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            prepareForm(model, request, "Tạo kế hoạch mới", "/trips");
            return "trips/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        TripPlan trip = tripPlanService.requireOwned(id, authentication.getName());
        prepareForm(model, tripPlanService.toRequest(trip), "Chỉnh sửa kế hoạch", "/trips/" + id,
                trip.getDestination().getId());
        return "trips/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("tripRequest") TripPlanRequest request,
                         BindingResult bindingResult, Authentication authentication, Model model,
                         RedirectAttributes redirectAttributes) {
        TripPlan currentTrip = tripPlanService.requireOwned(id, authentication.getName());
        Long currentDestinationId = currentTrip.getDestination().getId();
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Chỉnh sửa kế hoạch", "/trips/" + id, currentDestinationId);
            return "trips/form";
        }
        try {
            tripPlanService.update(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật kế hoạch");
            return "redirect:/trips/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            prepareForm(model, request, "Chỉnh sửa kế hoạch", "/trips/" + id, currentDestinationId);
            return "trips/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) ExpenseCategory expenseCategory,
                         @RequestParam(name = "tab", required = false) String requestedTab,
                         Authentication authentication, Model model) {
        TripPlan trip = tripPlanService.requireViewable(id, authentication.getName());
        BigDecimal total = detailService.totalExpense(trip);
        BigDecimal budget = trip.getBudget() == null ? BigDecimal.ZERO : trip.getBudget();
        BigDecimal percent = budget.compareTo(BigDecimal.ZERO) > 0
                ? total.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        List<ChecklistItem> checklist = detailService.checklist(trip);
        long checklistDone = checklist.stream().filter(ChecklistItem::isDone).count();
        int checklistPercent = checklist.isEmpty() ? 0
                : (int) Math.round(checklistDone * 100.0 / checklist.size());
        model.addAttribute("trip", trip);
        model.addAttribute("editable", trip.getOwner().getEmail().equalsIgnoreCase(authentication.getName()));
        model.addAttribute("itinerary", detailService.itinerary(trip));
        model.addAttribute("expenses", detailService.expenses(trip, expenseCategory));
        model.addAttribute("checklist", checklist);
        model.addAttribute("checklistDone", checklistDone);
        model.addAttribute("checklistPercent", checklistPercent);
        model.addAttribute("bookings", detailService.bookings(trip));
        model.addAttribute("totalExpense", total);
        model.addAttribute("budgetPercent", percent);
        model.addAttribute("budgetProgress", percent.min(BigDecimal.valueOf(100)));
        model.addAttribute("overBudget", budget.compareTo(BigDecimal.ZERO) > 0 && total.compareTo(budget) > 0);
        addIfMissing(model, "itineraryRequest", new ItineraryRequest());
        addIfMissing(model, "expenseRequest", new ExpenseRequest());
        addIfMissing(model, "checklistRequest", new ChecklistRequest());
        addIfMissing(model, "bookingRequest", new BookingNoteRequest());
        model.addAttribute("expenseCategories", ExpenseCategory.values());
        model.addAttribute("selectedExpenseCategory", expenseCategory);
        model.addAttribute("bookingTypes", BookingType.values());
        if (!model.containsAttribute("activeTripTab") && isTripTab(requestedTab)) {
            model.addAttribute("activeTripTab", requestedTab);
        }
        return "trips/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        tripPlanService.softDelete(id, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Đã xóa kế hoạch");
        return "redirect:/trips";
    }

    @PostMapping("/{id}/itinerary")
    public String addItinerary(@PathVariable Long id,
                               @Valid @ModelAttribute("itineraryRequest") ItineraryRequest request,
                               BindingResult result,
                               Authentication authentication, RedirectAttributes ra) {
        return executeDetailAction(id, request, "itineraryRequest", "itinerary", result, ra,
                () -> detailService.addItinerary(id, request, authentication.getName()), "Đã thêm lịch trình");
    }
    @PostMapping("/{id}/itinerary/{itemId}/update")
    public String updateItinerary(@PathVariable Long id, @PathVariable Long itemId,
                                  @Valid @ModelAttribute("itineraryRequest") ItineraryRequest request,
                                  BindingResult result,
                                  Authentication authentication, RedirectAttributes ra) {
        return executeDetailUpdate(id, itemId, request, "itineraryRequest", "itinerary",
                "editingItineraryId", result, ra,
                () -> detailService.updateItinerary(id, itemId, request, authentication.getName()),
                "Đã cập nhật lịch trình");
    }
    @PostMapping("/{id}/itinerary/{itemId}/delete")
    public String deleteItinerary(@PathVariable Long id, @PathVariable Long itemId,
                                  Authentication authentication, RedirectAttributes ra) {
        return execute(id, authentication, ra, "itinerary",
                () -> detailService.deleteItinerary(id, itemId, authentication.getName()), "Đã xóa lịch trình");
    }

    @PostMapping("/{id}/expenses")
    public String addExpense(@PathVariable Long id, @Valid ExpenseRequest request, BindingResult result,
                             Authentication authentication, RedirectAttributes ra) {
        return executeDetailAction(id, request, "expenseRequest", "expenses", result, ra,
                () -> detailService.addExpense(id, request, authentication.getName()), "Đã thêm khoản chi");
    }
    @PostMapping("/{id}/expenses/{expenseId}/update")
    public String updateExpense(@PathVariable Long id, @PathVariable Long expenseId,
                                @Valid @ModelAttribute("expenseRequest") ExpenseRequest request,
                                BindingResult result,
                                @RequestParam(required = false) ExpenseCategory expenseCategory,
                                Authentication authentication, RedirectAttributes ra) {
        ra.addAttribute("tab", "expenses");
        if (expenseCategory != null) {
            ra.addAttribute("expenseCategory", expenseCategory);
        }
        return executeDetailUpdate(id, expenseId, request, "expenseRequest", "expenses",
                "editingExpenseId", result, ra,
                () -> detailService.updateExpense(id, expenseId, request, authentication.getName()),
                "Đã cập nhật khoản chi");
    }
    @PostMapping("/{id}/expenses/{expenseId}/delete")
    public String deleteExpense(@PathVariable Long id, @PathVariable Long expenseId,
                                Authentication authentication, RedirectAttributes ra) {
        return execute(id, authentication, ra, "expenses",
                () -> detailService.deleteExpense(id, expenseId, authentication.getName()), "Đã xóa khoản chi");
    }

    @PostMapping("/{id}/checklist")
    public String addChecklist(@PathVariable Long id, @Valid ChecklistRequest request, BindingResult result,
                               Authentication authentication, RedirectAttributes ra) {
        return executeDetailAction(id, request, "checklistRequest", "checklist", result, ra,
                () -> detailService.addChecklist(id, request, authentication.getName()), "Đã thêm checklist");
    }
    @PostMapping("/{id}/checklist/{itemId}/update")
    public String updateChecklist(@PathVariable Long id, @PathVariable Long itemId,
                                  @Valid @ModelAttribute("checklistRequest") ChecklistRequest request,
                                  BindingResult result,
                                  Authentication authentication, RedirectAttributes ra) {
        return executeDetailUpdate(id, itemId, request, "checklistRequest", "checklist",
                "editingChecklistId", result, ra,
                () -> detailService.updateChecklist(id, itemId, request, authentication.getName()),
                "Đã cập nhật checklist");
    }
    @PostMapping("/{id}/checklist/{itemId}/toggle")
    public String toggleChecklist(@PathVariable Long id, @PathVariable Long itemId,
                                  @RequestParam Boolean done,
                                  Authentication authentication, RedirectAttributes ra) {
        return execute(id, authentication, ra,
                "checklist",
                () -> detailService.setChecklistDone(id, itemId, done, authentication.getName()),
                "Đã cập nhật checklist");
    }
    @PostMapping("/{id}/checklist/{itemId}/delete")
    public String deleteChecklist(@PathVariable Long id, @PathVariable Long itemId,
                                  Authentication authentication, RedirectAttributes ra) {
        return execute(id, authentication, ra, "checklist",
                () -> detailService.deleteChecklist(id, itemId, authentication.getName()), "Đã xóa checklist");
    }

    @PostMapping("/{id}/bookings")
    public String addBooking(@PathVariable Long id, @Valid BookingNoteRequest request, BindingResult result,
                             Authentication authentication, RedirectAttributes ra) {
        return executeDetailAction(id, request, "bookingRequest", "bookings", result, ra,
                () -> detailService.addBooking(id, request, authentication.getName()), "Đã thêm ghi chú đặt dịch vụ");
    }
    @PostMapping("/{id}/bookings/{bookingId}/update")
    public String updateBooking(@PathVariable Long id, @PathVariable Long bookingId,
                                @Valid @ModelAttribute("bookingRequest") BookingNoteRequest request,
                                BindingResult result,
                                Authentication authentication, RedirectAttributes ra) {
        return executeDetailUpdate(id, bookingId, request, "bookingRequest", "bookings",
                "editingBookingId", result, ra,
                () -> detailService.updateBooking(id, bookingId, request, authentication.getName()),
                "Đã cập nhật ghi chú đặt dịch vụ");
    }
    @PostMapping("/{id}/bookings/{bookingId}/delete")
    public String deleteBooking(@PathVariable Long id, @PathVariable Long bookingId,
                                Authentication authentication, RedirectAttributes ra) {
        return execute(id, authentication, ra, "bookings",
                () -> detailService.deleteBooking(id, bookingId, authentication.getName()), "Đã xóa ghi chú đặt dịch vụ");
    }

    private void prepareForm(Model model, TripPlanRequest request, String title, String action) {
        prepareForm(model, request, title, action, null);
    }

    private void prepareForm(Model model, TripPlanRequest request, String title, String action,
                             Long currentDestinationId) {
        List<Destination> destinations = new ArrayList<>(destinationService.activeDestinations());
        if (currentDestinationId != null
                && destinations.stream().noneMatch(destination -> currentDestinationId.equals(destination.getId()))) {
            destinations.add(destinationService.require(currentDestinationId));
            destinations.sort(Comparator.comparing(Destination::getName, String.CASE_INSENSITIVE_ORDER));
        }
        model.addAttribute("tripRequest", request);
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("destinations", destinations);
        model.addAttribute("statuses", TripStatus.values());
    }

    private String executeDetailAction(Long id, Object form, String formName, String tab,
                                       BindingResult result, RedirectAttributes ra,
                                       Runnable action, String success) {
        if (result.hasErrors()) {
            return redirectWithValidation(id, form, formName, tab, result, ra);
        }
        try {
            action.run();
            ra.addFlashAttribute("success", success);
        } catch (IllegalArgumentException ex) {
            result.reject("business", ex.getMessage());
            return redirectWithValidation(id, form, formName, tab, result, ra);
        }
        ra.addAttribute("tab", tab);
        return "redirect:/trips/" + id;
    }

    private String executeDetailUpdate(Long tripId, Long itemId, Object form, String formName,
                                       String tab, String editingIdAttribute,
                                       BindingResult result, RedirectAttributes ra,
                                       Runnable action, String success) {
        if (result.hasErrors()) {
            return redirectWithValidation(tripId, form, formName, tab, result, ra,
                    editingIdAttribute, itemId);
        }
        try {
            action.run();
            ra.addFlashAttribute("success", success);
        } catch (IllegalArgumentException ex) {
            result.reject("business", ex.getMessage());
            return redirectWithValidation(tripId, form, formName, tab, result, ra,
                    editingIdAttribute, itemId);
        }
        ra.addAttribute("tab", tab);
        return "redirect:/trips/" + tripId;
    }

    private String execute(Long id, Authentication authentication, RedirectAttributes ra,
                           String tab, Runnable action, String success) {
        try {
            action.run();
            ra.addFlashAttribute("success", success);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        ra.addAttribute("tab", tab);
        return "redirect:/trips/" + id;
    }

    private String redirectWithValidation(Long id, Object form, String formName, String tab,
                                          BindingResult result, RedirectAttributes ra) {
        return redirectWithValidation(id, form, formName, tab, result, ra, null, null);
    }

    private String redirectWithValidation(Long id, Object form, String formName, String tab,
                                          BindingResult result, RedirectAttributes ra,
                                          String editingIdAttribute, Long editingId) {
        ra.addFlashAttribute(formName, form);
        ra.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + formName, result);
        ra.addFlashAttribute("activeTripTab", tab);
        ra.addAttribute("tab", tab);
        if (editingIdAttribute != null && editingId != null) {
            ra.addFlashAttribute(editingIdAttribute, editingId);
        }
        return "redirect:/trips/" + id;
    }

    private void addIfMissing(Model model, String name, Object value) {
        if (!model.containsAttribute(name)) {
            model.addAttribute(name, value);
        }
    }

    private boolean isTripTab(String tab) {
        return "overview".equals(tab) || "itinerary".equals(tab) || "expenses".equals(tab)
                || "checklist".equals(tab) || "bookings".equals(tab);
    }
}
