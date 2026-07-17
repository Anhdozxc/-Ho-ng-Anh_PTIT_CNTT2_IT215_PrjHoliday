package vn.edu.ptit.holidayplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.repository.ChecklistItemRepository;
import vn.edu.ptit.holidayplanner.repository.ExpenseRepository;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final List<TripStatus> UPCOMING_EXCLUDED_STATUSES =
            List.of(TripStatus.CANCELLED, TripStatus.COMPLETED);

    private final UserService userService;
    private final TripPlanRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final ChecklistItemRepository checklistRepository;

    public DashboardService(UserService userService, TripPlanRepository tripRepository,
                            ExpenseRepository expenseRepository, ChecklistItemRepository checklistRepository) {
        this.userService = userService;
        this.tripRepository = tripRepository;
        this.expenseRepository = expenseRepository;
        this.checklistRepository = checklistRepository;
    }

    public DashboardStats stats(String email) {
        UserAccount user = userService.requireByEmail(email);
        boolean admin = user.getRole() == Role.ADMIN;
        long totalTrips = admin ? tripRepository.countByDeletedFalse() : tripRepository.countByOwnerAndDeletedFalse(user);
        BigDecimal totalExpense = admin ? expenseRepository.sumAllActiveTrips() : expenseRepository.sumByOwner(user);
        long openChecklist = admin ? checklistRepository.countOpenAll() : checklistRepository.countOpenByOwner(user);
        LocalDate today = LocalDate.now();
        List<TripPlan> upcoming = admin
                ? tripRepository.findTop5ByDeletedFalseAndStartDateGreaterThanEqualAndStatusNotInOrderByStartDateAscIdAsc(
                        today, UPCOMING_EXCLUDED_STATUSES)
                : tripRepository.findTop5ByOwnerAndDeletedFalseAndStartDateGreaterThanEqualAndStatusNotInOrderByStartDateAscIdAsc(
                        user, today, UPCOMING_EXCLUDED_STATUSES);
        Map<ExpenseCategory, BigDecimal> expenseByCategory = expenseBreakdown(user, admin);
        Map<TripStatus, Long> tripsByStatus = statusBreakdown(user, admin);
        BigDecimal maxCategoryExpense = expenseByCategory.values().stream()
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long maxStatusCount = tripsByStatus.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        return new DashboardStats(totalTrips, totalExpense, openChecklist, upcoming, admin,
                expenseByCategory, tripsByStatus, maxCategoryExpense, maxStatusCount);
    }

    private Map<ExpenseCategory, BigDecimal> expenseBreakdown(UserAccount user, boolean admin) {
        Map<ExpenseCategory, BigDecimal> result = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) result.put(category, BigDecimal.ZERO);
        List<Object[]> rows = admin ? expenseRepository.sumByCategoryAll()
                : expenseRepository.sumByCategoryForOwner(user);
        for (Object[] row : rows) result.put((ExpenseCategory) row[0], (BigDecimal) row[1]);
        return result;
    }

    private Map<TripStatus, Long> statusBreakdown(UserAccount user, boolean admin) {
        Map<TripStatus, Long> result = new EnumMap<>(TripStatus.class);
        for (TripStatus status : TripStatus.values()) result.put(status, 0L);
        List<Object[]> rows = admin ? tripRepository.countByStatusAll()
                : tripRepository.countByStatusForOwner(user);
        for (Object[] row : rows) result.put((TripStatus) row[0], (Long) row[1]);
        return result;
    }

    public record DashboardStats(long totalTrips, BigDecimal totalExpense, long openChecklist,
                                 List<TripPlan> upcomingTrips, boolean adminView,
                                 Map<ExpenseCategory, BigDecimal> expenseByCategory,
                                 Map<TripStatus, Long> tripsByStatus,
                                 BigDecimal maxCategoryExpense, long maxStatusCount) {}
}
