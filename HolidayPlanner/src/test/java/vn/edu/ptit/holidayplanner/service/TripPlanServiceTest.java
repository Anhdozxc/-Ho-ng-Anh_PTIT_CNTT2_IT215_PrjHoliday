package vn.edu.ptit.holidayplanner.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.dto.TripPlanRequest;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TripPlanServiceTest {
    @Autowired private TripPlanService tripPlanService;
    @Autowired private DestinationService destinationService;
    @Autowired private UserService userService;
    @Autowired private TripPlanRepository tripPlanRepository;

    @Test
    void createsAndSoftDeletesOwnedTrip() {
        Destination destination = destinationService.activeDestinations().get(0);
        TripPlanRequest request = validRequest(destination.getId());

        TripPlan saved = tripPlanService.create(request, "user@holidayplanner.vn");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Chuyến đi kiểm thử");
        assertThat(saved.getBudget()).isEqualByComparingTo("5000000.00");
        assertThat(tripPlanService.listOwned("user@holidayplanner.vn"))
                .extracting(TripPlan::getId).contains(saved.getId());

        tripPlanService.softDelete(saved.getId(), "user@holidayplanner.vn");
        assertThat(tripPlanService.listOwned("user@holidayplanner.vn"))
                .extracting(TripPlan::getId).doesNotContain(saved.getId());
    }

    @Test
    void endDateBeforeStartDateIsRejected() {
        Destination destination = destinationService.activeDestinations().get(0);
        TripPlanRequest request = validRequest(destination.getId());
        request.setEndDate(request.getStartDate().minusDays(1));

        assertThatThrownBy(() -> tripPlanService.create(request, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày kết thúc");
    }

    @Test
    void userCannotAccessAnotherUsersTrip() {
        Destination destination = destinationService.activeDestinations().get(0);
        TripPlan saved = tripPlanService.create(validRequest(destination.getId()), "user@holidayplanner.vn");

        assertThatThrownBy(() -> tripPlanService.requireOwned(saved.getId(), "admin@holidayplanner.vn"))
                .hasMessageContaining("không có quyền");
    }

    @Test
    void rejectsBudgetOutsideDatabasePrecisionWithoutRounding() {
        Destination destination = destinationService.activeDestinations().get(0);
        TripPlanRequest tooPrecise = validRequest(destination.getId());
        tooPrecise.setBudget(new BigDecimal("100.001"));
        TripPlanRequest tooLarge = validRequest(destination.getId());
        tooLarge.setBudget(new BigDecimal("10000000000000.00"));

        assertThatThrownBy(() -> tripPlanService.create(tooPrecise, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13 chữ số nguyên và 2 chữ số thập phân");
        assertThatThrownBy(() -> tripPlanService.create(tooLarge, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13 chữ số nguyên và 2 chữ số thập phân");
    }

    @Test
    void optionalBudgetIsNormalizedToZero() {
        Destination destination = destinationService.activeDestinations().get(0);
        TripPlanRequest request = validRequest(destination.getId());
        request.setBudget(null);

        TripPlan saved = tripPlanService.create(request, "user@holidayplanner.vn");

        assertThat(saved.getBudget()).isEqualByComparingTo("0.00");
    }

    @Test
    void searchesAndPaginatesAtRepositoryLevel() {
        Destination destination = destinationService.activeDestinations().get(0);
        IntStream.rangeClosed(1, 5).forEach(index -> {
            TripPlanRequest request = validRequest(destination.getId());
            request.setTitle("Phân trang riêng " + index);
            request.setStartDate(LocalDate.of(2026, 10, index));
            request.setEndDate(LocalDate.of(2026, 10, index + 1));
            tripPlanService.create(request, "user@holidayplanner.vn");
        });

        var firstPage = tripPlanService.searchVisible(
                "user@holidayplanner.vn", "PHÂN TRANG RIÊNG", TripStatus.PLANNED,
                "title", "asc", 0, 2);
        var lastPage = tripPlanService.searchVisible(
                "user@holidayplanner.vn", "phân trang riêng", TripStatus.PLANNED,
                "title", "asc", 2, 2);

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(TripPlan::getTitle)
                .containsExactly("Phân trang riêng 1", "Phân trang riêng 2");
        assertThat(lastPage.getContent()).extracting(TripPlan::getTitle)
                .containsExactly("Phân trang riêng 5");
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowedStatusTransitions")
    void allowsEveryDocumentedStatusTransitionAndSameStatusEdits(
            TripStatus currentStatus, TripStatus requestedStatus) {
        TripPlan trip = persistedTrip(currentStatus);
        TripPlanRequest update = tripPlanService.toRequest(trip);
        update.setTitle("Updated " + currentStatus + " to " + requestedStatus);
        update.setStatus(requestedStatus);

        TripPlan updated = tripPlanService.update(
                trip.getId(), update, "user@holidayplanner.vn");

        assertThat(updated.getStatus()).isEqualTo(requestedStatus);
        assertThat(updated.getTitle()).isEqualTo("Updated " + currentStatus + " to " + requestedStatus);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @MethodSource("invalidStatusTransitions")
    void rejectsEveryUndocumentedStatusTransition(
            TripStatus currentStatus, TripStatus requestedStatus) {
        TripPlan trip = persistedTrip(currentStatus);
        TripPlanRequest update = tripPlanService.toRequest(trip);
        update.setStatus(requestedStatus);

        assertThatThrownBy(() -> tripPlanService.update(
                trip.getId(), update, "user@holidayplanner.vn"))
                .isInstanceOf(ConflictException.class);
        assertThat(trip.getStatus()).isEqualTo(currentStatus);
    }

    private static Stream<Arguments> allowedStatusTransitions() {
        return statusPairs().filter(pair -> isAllowed(pair[0], pair[1]))
                .map(pair -> Arguments.of(pair[0], pair[1]));
    }

    private static Stream<Arguments> invalidStatusTransitions() {
        return statusPairs().filter(pair -> !isAllowed(pair[0], pair[1]))
                .map(pair -> Arguments.of(pair[0], pair[1]));
    }

    private static Stream<TripStatus[]> statusPairs() {
        return Arrays.stream(TripStatus.values())
                .flatMap(from -> Arrays.stream(TripStatus.values())
                        .map(to -> new TripStatus[]{from, to}));
    }

    private static boolean isAllowed(TripStatus from, TripStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case DRAFT -> to == TripStatus.PLANNED || to == TripStatus.CANCELLED;
            case PLANNED -> to == TripStatus.ONGOING || to == TripStatus.CANCELLED;
            case ONGOING -> to == TripStatus.COMPLETED || to == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private TripPlan persistedTrip(TripStatus status) {
        TripPlan trip = new TripPlan();
        trip.setTitle("Status matrix " + status);
        trip.setDestination(destinationService.activeDestinations().get(0));
        trip.setStartDate(LocalDate.of(2027, 4, 10));
        trip.setEndDate(LocalDate.of(2027, 4, 12));
        trip.setPeopleCount(2);
        trip.setBudget(new BigDecimal("1000.00"));
        trip.setStatus(status);
        trip.setOwner(userService.requireByEmail("user@holidayplanner.vn"));
        return tripPlanRepository.saveAndFlush(trip);
    }

    private TripPlanRequest validRequest(Long destinationId) {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Chuyến đi kiểm thử");
        request.setDestinationId(destinationId);
        request.setStartDate(LocalDate.of(2026, 8, 10));
        request.setEndDate(LocalDate.of(2026, 8, 12));
        request.setPeopleCount(2);
        request.setBudget(new BigDecimal("5000000"));
        request.setStatus(TripStatus.PLANNED);
        request.setNotes("Dữ liệu kiểm thử");
        return request;
    }
}
