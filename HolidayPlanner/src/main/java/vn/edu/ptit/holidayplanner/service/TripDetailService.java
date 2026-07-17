package vn.edu.ptit.holidayplanner.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.*;
import vn.edu.ptit.holidayplanner.dto.*;
import vn.edu.ptit.holidayplanner.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;

@Service
@Transactional
public class TripDetailService {
    private final TripPlanService tripPlanService;
    private final ItineraryItemRepository itineraryRepository;
    private final ExpenseRepository expenseRepository;
    private final ChecklistItemRepository checklistRepository;
    private final BookingNoteRepository bookingRepository;

    public TripDetailService(
            TripPlanService tripPlanService,
            ItineraryItemRepository itineraryRepository,
            ExpenseRepository expenseRepository,
            ChecklistItemRepository checklistRepository,
            BookingNoteRepository bookingRepository) {
        this.tripPlanService = tripPlanService;
        this.itineraryRepository = itineraryRepository;
        this.expenseRepository = expenseRepository;
        this.checklistRepository = checklistRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<ItineraryItem> itinerary(TripPlan trip) {
        return itineraryRepository.findByTripPlanOrderByDayNoAscFromTimeAsc(trip);
    }
    @Transactional(readOnly = true)
    public List<Expense> expenses(TripPlan trip) {
        return expenseRepository.findByTripPlanOrderBySpentDateDesc(trip);
    }
    @Transactional(readOnly = true)
    public List<Expense> expenses(TripPlan trip, ExpenseCategory category) {
        return category == null ? expenses(trip)
                : expenseRepository.findByTripPlanAndCategoryOrderBySpentDateDesc(trip, category);
    }
    @Transactional(readOnly = true)
    public List<ChecklistItem> checklist(TripPlan trip) {
        return checklistRepository.findByTripPlanOrderByDoneAscDueDateAsc(trip);
    }
    @Transactional(readOnly = true)
    public List<BookingNote> bookings(TripPlan trip) {
        return bookingRepository.findByTripPlanOrderByIdDesc(trip);
    }
    @Transactional(readOnly = true)
    public BigDecimal totalExpense(TripPlan trip) {
        BigDecimal total = expenseRepository.sumByTrip(trip);
        return total == null ? BigDecimal.ZERO.setScale(2) : total.setScale(2, RoundingMode.UNNECESSARY);
    }

    public ItineraryItem addItinerary(Long tripId, ItineraryRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        validateItinerary(trip, request, null);
        ItineraryItem item = new ItineraryItem();
        item.setTripPlan(trip);
        applyItinerary(item, request);
        return itineraryRepository.save(item);
    }

    public ItineraryItem updateItinerary(Long tripId, Long itemId, ItineraryRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ItineraryItem item = requireItinerary(trip, itemId);
        validateItinerary(trip, request, itemId);
        applyItinerary(item, request);
        return itineraryRepository.save(item);
    }

    private void validateItinerary(TripPlan trip, ItineraryRequest request, Long ignoredItemId) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu lịch trình là bắt buộc");
        }
        long maxDay = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        if (request.getDayNo() == null || request.getDayNo() < 1 || request.getDayNo() > maxDay) {
            throw new IllegalArgumentException("Ngày thứ phải nằm trong khoảng chuyến đi (1-" + maxDay + ")");
        }
        if ((request.getFromTime() == null) != (request.getToTime() == null)) {
            throw new IllegalArgumentException("Giờ bắt đầu và giờ kết thúc phải được nhập cùng nhau");
        }
        if (request.getFromTime() != null && !request.getToTime().isAfter(request.getFromTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (request.getFromTime() != null && request.getToTime() != null) {
            boolean overlaps = itineraryRepository.findByTripPlanOrderByDayNoAscFromTimeAsc(trip).stream()
                    .filter(existing -> !Objects.equals(existing.getId(), ignoredItemId))
                    .filter(existing -> Objects.equals(existing.getDayNo(), request.getDayNo()))
                    .filter(existing -> existing.getFromTime() != null && existing.getToTime() != null)
                    .anyMatch(existing -> request.getFromTime().isBefore(existing.getToTime())
                            && existing.getFromTime().isBefore(request.getToTime()));
            if (overlaps) {
                throw new ConflictException("Khoảng thời gian bị trùng với hoạt động khác trong cùng ngày");
            }
        }
        validateRequiredText(request.getActivity(), 250, "Hoạt động");
        validateOptionalText(request.getLocation(), 250, "Địa điểm");
        validateOptionalText(request.getNote(), 1200, "Ghi chú");
    }

    private void applyItinerary(ItineraryItem item, ItineraryRequest request) {
        item.setDayNo(request.getDayNo());
        item.setFromTime(request.getFromTime());
        item.setToTime(request.getToTime());
        item.setActivity(request.getActivity().trim());
        item.setLocation(trimToNull(request.getLocation()));
        item.setNote(trimToNull(request.getNote()));
    }

    public void deleteItinerary(Long tripId, Long itemId, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ItineraryItem item = requireItinerary(trip, itemId);
        itineraryRepository.delete(item);
    }

    public Expense addExpense(Long tripId, ExpenseRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        validateExpense(trip, request);
        Expense expense = new Expense();
        expense.setTripPlan(trip);
        applyExpense(expense, request);
        return expenseRepository.save(expense);
    }

    public Expense updateExpense(Long tripId, Long expenseId, ExpenseRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        Expense expense = requireExpense(trip, expenseId);
        validateExpense(trip, request);
        applyExpense(expense, request);
        return expenseRepository.save(expense);
    }

    private void applyExpense(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.UNNECESSARY));
        expense.setSpentDate(request.getSpentDate());
        expense.setNote(trimToNull(request.getNote()));
    }

    public void deleteExpense(Long tripId, Long expenseId, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        Expense expense = requireExpense(trip, expenseId);
        expenseRepository.delete(expense);
    }

    public ChecklistItem addChecklist(Long tripId, ChecklistRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        validateChecklist(request);
        ChecklistItem item = new ChecklistItem();
        item.setTripPlan(trip);
        applyChecklist(item, request);
        item.setDone(false);
        return checklistRepository.save(item);
    }

    public ChecklistItem updateChecklist(Long tripId, Long itemId, ChecklistRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ChecklistItem item = requireChecklist(trip, itemId);
        validateChecklist(request);
        applyChecklist(item, request);
        return checklistRepository.save(item);
    }

    private void applyChecklist(ChecklistItem item, ChecklistRequest request) {
        item.setTitle(request.getTitle().trim());
        item.setCategory(trimToNull(request.getCategory()));
        item.setDueDate(request.getDueDate());
    }

    public ChecklistItem toggleChecklist(Long tripId, Long itemId, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ChecklistItem item = requireChecklist(trip, itemId);
        return setChecklistDone(item, !item.isDone());
    }

    public ChecklistItem setChecklistDone(Long tripId, Long itemId, boolean done, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ChecklistItem item = requireChecklist(trip, itemId);
        return setChecklistDone(item, done);
    }

    private ChecklistItem setChecklistDone(ChecklistItem item, boolean done) {
        item.setDone(done);
        return checklistRepository.save(item);
    }

    public void deleteChecklist(Long tripId, Long itemId, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        ChecklistItem item = requireChecklist(trip, itemId);
        checklistRepository.delete(item);
    }

    public BookingNote addBooking(Long tripId, BookingNoteRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        validateBooking(request);
        BookingNote booking = new BookingNote();
        booking.setTripPlan(trip);
        applyBooking(booking, request);
        return bookingRepository.save(booking);
    }

    public BookingNote updateBooking(Long tripId, Long bookingId, BookingNoteRequest request, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        BookingNote booking = requireBooking(trip, bookingId);
        validateBooking(request);
        applyBooking(booking, request);
        return bookingRepository.save(booking);
    }

    private void applyBooking(BookingNote booking, BookingNoteRequest request) {
        booking.setType(request.getType());
        booking.setProvider(request.getProvider().trim());
        booking.setBookingCode(trimToNull(request.getBookingCode()));
        booking.setPrice(request.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        booking.setNote(trimToNull(request.getNote()));
    }

    public void deleteBooking(Long tripId, Long bookingId, String email) {
        TripPlan trip = tripPlanService.requireOwned(tripId, email);
        BookingNote booking = requireBooking(trip, bookingId);
        bookingRepository.delete(booking);
    }

    private ItineraryItem requireItinerary(TripPlan requestedTrip, Long itemId) {
        ItineraryItem item = itineraryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch trình"));
        requireChildTrip(requestedTrip, item.getTripPlan(), "Không tìm thấy lịch trình");
        return item;
    }

    private Expense requireExpense(TripPlan requestedTrip, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khoản chi"));
        requireChildTrip(requestedTrip, expense.getTripPlan(), "Không tìm thấy khoản chi");
        return expense;
    }

    private ChecklistItem requireChecklist(TripPlan requestedTrip, Long itemId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy checklist"));
        requireChildTrip(requestedTrip, item.getTripPlan(), "Không tìm thấy checklist");
        return item;
    }

    private BookingNote requireBooking(TripPlan requestedTrip, Long bookingId) {
        BookingNote booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ghi chú đặt dịch vụ"));
        requireChildTrip(requestedTrip, booking.getTripPlan(), "Không tìm thấy ghi chú đặt dịch vụ");
        return booking;
    }

    private void requireChildTrip(TripPlan requestedTrip, TripPlan actualTrip, String notFoundMessage) {
        if (Objects.equals(requestedTrip.getId(), actualTrip.getId())) {
            return;
        }
        if (!Objects.equals(requestedTrip.getOwner().getId(), actualTrip.getOwner().getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu chuyến đi này");
        }
        throw new EntityNotFoundException(notFoundMessage);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateExpense(TripPlan trip, ExpenseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu khoản chi là bắt buộc");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Danh mục là bắt buộc");
        }
        validateMoney(request.getAmount(), "Số tiền");
        if (request.getSpentDate() != null
                && (request.getSpentDate().isBefore(trip.getStartDate())
                || request.getSpentDate().isAfter(trip.getEndDate()))) {
            throw new IllegalArgumentException("Ngày chi phải nằm trong khoảng thời gian chuyến đi");
        }
        validateOptionalText(request.getNote(), 1200, "Ghi chú");
    }

    private void validateChecklist(ChecklistRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu checklist là bắt buộc");
        }
        validateRequiredText(request.getTitle(), 250, "Tên việc");
        validateOptionalText(request.getCategory(), 100, "Danh mục");
    }

    private void validateBooking(BookingNoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đặt dịch vụ là bắt buộc");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Loại dịch vụ là bắt buộc");
        }
        validateRequiredText(request.getProvider(), 180, "Nhà cung cấp");
        validateOptionalText(request.getBookingCode(), 120, "Mã đặt chỗ");
        validateMoney(request.getPrice(), "Chi phí đặt dịch vụ");
        validateOptionalText(request.getNote(), 1200, "Ghi chú");
    }

    private void validateMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " là bắt buộc");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " không được âm");
        }
        BigDecimal normalized = value.stripTrailingZeros();
        int fractionDigits = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        if (integerDigits > 13 || fractionDigits > 2) {
            throw new IllegalArgumentException(fieldName + " tối đa 13 chữ số nguyên và 2 chữ số thập phân");
        }
    }

    private void validateRequiredText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " tối đa " + maxLength + " ký tự");
        }
    }

    private void validateOptionalText(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " tối đa " + maxLength + " ký tự");
        }
    }
}
