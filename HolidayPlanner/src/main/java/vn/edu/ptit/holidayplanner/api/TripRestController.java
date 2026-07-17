package vn.edu.ptit.holidayplanner.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.dto.*;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.service.TripDetailService;
import vn.edu.ptit.holidayplanner.service.TripPlanService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripRestController {
    private final TripPlanService tripPlanService;
    private final TripDetailService detailService;

    public TripRestController(TripPlanService tripPlanService, TripDetailService detailService) {
        this.tripPlanService = tripPlanService;
        this.detailService = detailService;
    }

    @GetMapping
    public PageResponse<Map<String,Object>> list(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) TripStatus status,
                                                 @RequestParam(defaultValue = "startDate") String sort,
                                                 @RequestParam(defaultValue = "desc") String direction,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 Authentication auth) {
        return PageResponse.from(
                tripPlanService.searchVisible(auth.getName(), q, status, sort, direction, page, size),
                ApiMapper::trip);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@Valid @RequestBody TripPlanRequest request, Authentication auth) {
        return ApiMapper.trip(tripPlanService.create(request, auth.getName()));
    }

    @GetMapping("/{id}")
    public Map<String,Object> detail(@PathVariable Long id,
                                     @RequestParam(required = false) ExpenseCategory expenseCategory,
                                     Authentication auth) {
        TripPlan trip = tripPlanService.requireViewable(id, auth.getName());
        Map<String,Object> m = new LinkedHashMap<>(ApiMapper.trip(trip));
        m.put("itinerary", detailService.itinerary(trip).stream().map(ApiMapper::itinerary).toList());
        m.put("expenses", detailService.expenses(trip, expenseCategory).stream().map(ApiMapper::expense).toList());
        m.put("checklist", detailService.checklist(trip).stream().map(ApiMapper::checklist).toList());
        m.put("bookings", detailService.bookings(trip).stream().map(ApiMapper::booking).toList());
        m.put("totalExpense", detailService.totalExpense(trip));
        return m;
    }

    @PutMapping("/{id}")
    public Map<String,Object> update(@PathVariable Long id, @Valid @RequestBody TripPlanRequest request, Authentication auth) {
        return ApiMapper.trip(tripPlanService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        tripPlanService.softDelete(id, auth.getName());
    }

    @PostMapping("/{id}/itinerary")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addItinerary(@PathVariable Long id, @Valid @RequestBody ItineraryRequest request, Authentication auth) {
        return ApiMapper.itinerary(detailService.addItinerary(id, request, auth.getName()));
    }

    @PutMapping("/{id}/itinerary/{itemId}")
    public Map<String,Object> updateItinerary(@PathVariable Long id, @PathVariable Long itemId,
                                              @Valid @RequestBody ItineraryRequest request, Authentication auth) {
        return ApiMapper.itinerary(detailService.updateItinerary(id, itemId, request, auth.getName()));
    }

    @DeleteMapping("/{id}/itinerary/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItinerary(@PathVariable Long id, @PathVariable Long itemId, Authentication auth) {
        detailService.deleteItinerary(id, itemId, auth.getName());
    }

    @PostMapping("/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request, Authentication auth) {
        return ApiMapper.expense(detailService.addExpense(id, request, auth.getName()));
    }

    @PutMapping("/{id}/expenses/{expenseId}")
    public Map<String,Object> updateExpense(@PathVariable Long id, @PathVariable Long expenseId,
                                            @Valid @RequestBody ExpenseRequest request, Authentication auth) {
        return ApiMapper.expense(detailService.updateExpense(id, expenseId, request, auth.getName()));
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id, @PathVariable Long expenseId, Authentication auth) {
        detailService.deleteExpense(id, expenseId, auth.getName());
    }

    @PostMapping("/{id}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addChecklist(@PathVariable Long id, @Valid @RequestBody ChecklistRequest request, Authentication auth) {
        return ApiMapper.checklist(detailService.addChecklist(id, request, auth.getName()));
    }

    @PutMapping("/{id}/checklist/{itemId}")
    public Map<String,Object> updateChecklist(@PathVariable Long id, @PathVariable Long itemId,
                                              @Valid @RequestBody ChecklistRequest request, Authentication auth) {
        return ApiMapper.checklist(detailService.updateChecklist(id, itemId, request, auth.getName()));
    }

    @PatchMapping({"/{id}/checklist/{itemId}/state", "/{id}/checklist/{itemId}/toggle"})
    public Map<String,Object> setChecklistState(@PathVariable Long id, @PathVariable Long itemId,
                                                @Valid @RequestBody ChecklistStateRequest request,
                                                Authentication auth) {
        return ApiMapper.checklist(detailService.setChecklistDone(
                id, itemId, request.getDone(), auth.getName()));
    }

    @DeleteMapping("/{id}/checklist/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChecklist(@PathVariable Long id, @PathVariable Long itemId, Authentication auth) {
        detailService.deleteChecklist(id, itemId, auth.getName());
    }

    @PostMapping("/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> addBooking(@PathVariable Long id, @Valid @RequestBody BookingNoteRequest request, Authentication auth) {
        return ApiMapper.booking(detailService.addBooking(id, request, auth.getName()));
    }

    @PutMapping("/{id}/bookings/{bookingId}")
    public Map<String,Object> updateBooking(@PathVariable Long id, @PathVariable Long bookingId,
                                            @Valid @RequestBody BookingNoteRequest request, Authentication auth) {
        return ApiMapper.booking(detailService.updateBooking(id, bookingId, request, auth.getName()));
    }

    @DeleteMapping("/{id}/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable Long id, @PathVariable Long bookingId, Authentication auth) {
        detailService.deleteBooking(id, bookingId, auth.getName());
    }
}
