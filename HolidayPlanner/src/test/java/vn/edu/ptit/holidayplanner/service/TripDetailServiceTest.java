package vn.edu.ptit.holidayplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.ChecklistItem;
import vn.edu.ptit.holidayplanner.domain.Expense;
import vn.edu.ptit.holidayplanner.domain.ItineraryItem;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TripDetailServiceTest {
    @Autowired private TripPlanService tripPlanService;
    @Autowired private TripDetailService detailService;
    @Autowired private DestinationService destinationService;
    private TripPlan trip;

    @BeforeEach
    void createTrip() {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Đà Nẵng kiểm thử");
        request.setDestinationId(destinationService.activeDestinations().get(0).getId());
        request.setStartDate(LocalDate.of(2026, 9, 10));
        request.setEndDate(LocalDate.of(2026, 9, 12));
        request.setPeopleCount(2);
        request.setBudget(new BigDecimal("3000000"));
        request.setStatus(TripStatus.PLANNED);
        trip = tripPlanService.create(request, "user@holidayplanner.vn");
    }

    @Test
    void itineraryRejectsInvalidDayAndOverlappingTime() {
        ItineraryRequest first = itinerary(1, "09:00", "10:30", "Tham quan");
        detailService.addItinerary(trip.getId(), first, "user@holidayplanner.vn");

        ItineraryRequest overlap = itinerary(1, "10:00", "11:00", "Ăn sáng muộn");
        assertThatThrownBy(() -> detailService.addItinerary(trip.getId(), overlap, "user@holidayplanner.vn"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("trùng");

        ItineraryRequest outside = itinerary(4, "09:00", "10:00", "Ngoài chuyến");
        assertThatThrownBy(() -> detailService.addItinerary(trip.getId(), outside, "user@holidayplanner.vn"))
                .hasMessageContaining("1-3");

        ItineraryRequest backwards = itinerary(2, "11:00", "10:00", "Sai khung giờ");
        assertThatThrownBy(() -> detailService.addItinerary(trip.getId(), backwards, "user@holidayplanner.vn"))
                .hasMessageContaining("kết thúc phải sau");
    }

    @Test
    void itineraryRequiresCompleteTimePairAndUpdateIgnoresItself() {
        ItineraryItem saved = detailService.addItinerary(
                trip.getId(), itinerary(1, "09:00", "10:30", "Tham quan"), "user@holidayplanner.vn");

        ItineraryRequest unchanged = itinerary(1, "09:00", "10:30", "Tham quan cập nhật");
        assertThat(detailService.updateItinerary(
                trip.getId(), saved.getId(), unchanged, "user@holidayplanner.vn").getActivity())
                .isEqualTo("Tham quan cập nhật");

        ItineraryRequest missingEnd = itinerary(2, "11:00", "12:00", "Ăn trưa");
        missingEnd.setToTime(null);
        assertThatThrownBy(() -> detailService.addItinerary(
                trip.getId(), missingEnd, "user@holidayplanner.vn"))
                .hasMessageContaining("phải được nhập cùng nhau");
    }

    @Test
    void expenseUsesBigDecimalAndCalculatesTotal() {
        ExpenseRequest food = expense("120000.25", ExpenseCategory.FOOD);
        var saved = detailService.addExpense(trip.getId(), food, "user@holidayplanner.vn");
        ExpenseRequest updated = expense("150000.50", ExpenseCategory.FOOD);
        detailService.updateExpense(trip.getId(), saved.getId(), updated, "user@holidayplanner.vn");
        detailService.addExpense(trip.getId(), expense("200000", ExpenseCategory.TRANSPORT), "user@holidayplanner.vn");

        assertThat(detailService.totalExpense(trip)).isEqualByComparingTo("350000.50");
        assertThat(detailService.expenses(trip, ExpenseCategory.FOOD)).hasSize(1);

        detailService.deleteExpense(trip.getId(), saved.getId(), "user@holidayplanner.vn");
        assertThat(detailService.totalExpense(trip)).isEqualByComparingTo("200000.00");
    }

    @Test
    void expenseRejectsPrecisionInsteadOfSilentlyRounding() {
        assertThatThrownBy(() -> detailService.addExpense(
                trip.getId(), expense("123.456", ExpenseCategory.FOOD), "user@holidayplanner.vn"))
                .hasMessageContaining("13 chữ số nguyên và 2 chữ số thập phân");
    }

    @Test
    void expenseRejectsNegativeAmountAndAcceptsZero() {
        assertThatThrownBy(() -> detailService.addExpense(
                trip.getId(), expense("-0.01", ExpenseCategory.OTHER), "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class);

        Expense zeroExpense = detailService.addExpense(
                trip.getId(), expense("0.00", ExpenseCategory.OTHER), "user@holidayplanner.vn");
        assertThat(zeroExpense.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void spentDateIsOptionalAndOtherwiseMustBeInsideTripDatesInclusively() {
        ExpenseRequest beforeTrip = expense("1.00", ExpenseCategory.OTHER);
        beforeTrip.setSpentDate(trip.getStartDate().minusDays(1));
        ExpenseRequest afterTrip = expense("1.00", ExpenseCategory.OTHER);
        afterTrip.setSpentDate(trip.getEndDate().plusDays(1));

        assertThatThrownBy(() -> detailService.addExpense(
                trip.getId(), beforeTrip, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> detailService.addExpense(
                trip.getId(), afterTrip, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class);

        ExpenseRequest onStart = expense("1.00", ExpenseCategory.OTHER);
        onStart.setSpentDate(trip.getStartDate());
        ExpenseRequest onEnd = expense("1.00", ExpenseCategory.OTHER);
        onEnd.setSpentDate(trip.getEndDate());
        ExpenseRequest withoutDate = expense("1.00", ExpenseCategory.OTHER);
        withoutDate.setSpentDate(null);

        assertThat(detailService.addExpense(
                trip.getId(), onStart, "user@holidayplanner.vn").getSpentDate())
                .isEqualTo(trip.getStartDate());
        assertThat(detailService.addExpense(
                trip.getId(), onEnd, "user@holidayplanner.vn").getSpentDate())
                .isEqualTo(trip.getEndDate());
        assertThat(detailService.addExpense(
                trip.getId(), withoutDate, "user@holidayplanner.vn").getSpentDate())
                .isNull();
    }

    @Test
    void checklistCanBeUpdatedAndToggled() {
        ChecklistRequest request = new ChecklistRequest();
        request.setTitle("Chuẩn bị CCCD");
        ChecklistItem item = detailService.addChecklist(trip.getId(), request, "user@holidayplanner.vn");
        assertThat(item.isDone()).isFalse();
        request.setTitle("Chuẩn bị hộ chiếu");
        detailService.updateChecklist(trip.getId(), item.getId(), request, "user@holidayplanner.vn");
        ChecklistItem toggled = detailService.toggleChecklist(trip.getId(), item.getId(), "user@holidayplanner.vn");
        assertThat(toggled.getTitle()).isEqualTo("Chuẩn bị hộ chiếu");
        assertThat(toggled.isDone()).isTrue();

        ChecklistRequest pendingRequest = new ChecklistRequest();
        pendingRequest.setTitle("Mua thuốc");
        ChecklistItem pending = detailService.addChecklist(trip.getId(), pendingRequest, "user@holidayplanner.vn");
        assertThat(detailService.checklist(trip))
                .extracting(ChecklistItem::getId)
                .containsExactly(pending.getId(), toggled.getId());
    }

    @Test
    void checklistUpdatePreservesStateAndExplicitStateChangeIsIdempotent() {
        ChecklistRequest request = new ChecklistRequest();
        request.setTitle("Mang hộ chiếu");
        ChecklistItem item = detailService.addChecklist(trip.getId(), request, "user@holidayplanner.vn");
        detailService.setChecklistDone(trip.getId(), item.getId(), true, "user@holidayplanner.vn");

        ChecklistRequest update = new ChecklistRequest();
        update.setTitle("Mang hộ chiếu còn hạn");
        ChecklistItem updated = detailService.updateChecklist(
                trip.getId(), item.getId(), update, "user@holidayplanner.vn");
        assertThat(updated.isDone()).isTrue();

        detailService.setChecklistDone(trip.getId(), item.getId(), true, "user@holidayplanner.vn");
        ChecklistItem repeated = detailService.setChecklistDone(
                trip.getId(), item.getId(), true, "user@holidayplanner.vn");
        assertThat(repeated.isDone()).isTrue();
    }

    @Test
    void bookingValidatesPriceAndSupportsOptionalCode() {
        BookingNoteRequest request = new BookingNoteRequest();
        request.setType(BookingType.HOTEL);
        request.setProvider("Khách sạn Demo");
        request.setBookingCode(null);
        request.setPrice(new BigDecimal("900000"));
        var booking = detailService.addBooking(trip.getId(), request, "user@holidayplanner.vn");
        assertThat(booking.getBookingCode()).isNull();

        request.setPrice(new BigDecimal("-1"));
        assertThatThrownBy(() -> detailService.updateBooking(trip.getId(), booking.getId(), request,
                "user@holidayplanner.vn")).hasMessageContaining("không được âm");
    }

    @Test
    void childIdsFromAnotherTripAreRejectedAndOriginalDataRemains() {
        TripPlanRequest tripRequest = new TripPlanRequest();
        tripRequest.setTitle("Chuyến thứ hai");
        tripRequest.setDestinationId(destinationService.activeDestinations().get(0).getId());
        tripRequest.setStartDate(LocalDate.of(2026, 9, 20));
        tripRequest.setEndDate(LocalDate.of(2026, 9, 22));
        tripRequest.setPeopleCount(2);
        tripRequest.setBudget(new BigDecimal("3000000"));
        tripRequest.setStatus(TripStatus.PLANNED);
        TripPlan anotherTrip = tripPlanService.create(tripRequest, "user@holidayplanner.vn");

        ItineraryItem itinerary = detailService.addItinerary(
                trip.getId(), itinerary(1, "09:00", "10:00", "Bản gốc"), "user@holidayplanner.vn");
        Expense expense = detailService.addExpense(
                trip.getId(), expense("100000", ExpenseCategory.FOOD), "user@holidayplanner.vn");
        ChecklistRequest checklistRequest = new ChecklistRequest();
        checklistRequest.setTitle("Bản gốc");
        ChecklistItem checklist = detailService.addChecklist(
                trip.getId(), checklistRequest, "user@holidayplanner.vn");
        BookingNoteRequest bookingRequest = new BookingNoteRequest();
        bookingRequest.setType(BookingType.HOTEL);
        bookingRequest.setProvider("Bản gốc");
        bookingRequest.setPrice(BigDecimal.ZERO);
        var booking = detailService.addBooking(
                trip.getId(), bookingRequest, "user@holidayplanner.vn");

        assertThatThrownBy(() -> detailService.updateItinerary(
                anotherTrip.getId(), itinerary.getId(), itinerary(1, "11:00", "12:00", "Sai trip"),
                "user@holidayplanner.vn")).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        assertThatThrownBy(() -> detailService.updateExpense(
                anotherTrip.getId(), expense.getId(), expense("1", ExpenseCategory.OTHER),
                "user@holidayplanner.vn")).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        checklistRequest.setTitle("Sai trip");
        assertThatThrownBy(() -> detailService.updateChecklist(
                anotherTrip.getId(), checklist.getId(), checklistRequest,
                "user@holidayplanner.vn")).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        bookingRequest.setProvider("Sai trip");
        assertThatThrownBy(() -> detailService.updateBooking(
                anotherTrip.getId(), booking.getId(), bookingRequest,
                "user@holidayplanner.vn")).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        assertThat(detailService.itinerary(trip).get(0).getActivity()).isEqualTo("Bản gốc");
        assertThat(detailService.totalExpense(trip)).isEqualByComparingTo("100000.00");
        assertThat(detailService.checklist(trip).get(0).getTitle()).isEqualTo("Bản gốc");
        assertThat(detailService.bookings(trip).get(0).getProvider()).isEqualTo("Bản gốc");
    }

    private ItineraryRequest itinerary(int day, String from, String to, String activity) {
        ItineraryRequest request = new ItineraryRequest();
        request.setDayNo(day);
        request.setFromTime(LocalTime.parse(from));
        request.setToTime(LocalTime.parse(to));
        request.setActivity(activity);
        return request;
    }

    private ExpenseRequest expense(String amount, ExpenseCategory category) {
        ExpenseRequest request = new ExpenseRequest();
        request.setCategory(category);
        request.setAmount(new BigDecimal(amount));
        request.setSpentDate(LocalDate.of(2026, 9, 10));
        return request;
    }
}
