package vn.edu.ptit.holidayplanner.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.dto.TripPlanRequest;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;
import vn.edu.ptit.holidayplanner.repository.DestinationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardAndDestinationServiceTest {
    @Autowired private DashboardService dashboardService;
    @Autowired private DestinationService destinationService;
    @Autowired private TripPlanService tripPlanService;
    @Autowired private UserService userService;
    @Autowired private TripPlanRepository tripPlanRepository;
    @Autowired private DestinationRepository destinationRepository;

    @Test
    void dashboardSeparatesPersonalAndAdminScope() {
        var userStats = dashboardService.stats("user@holidayplanner.vn");
        var adminStats = dashboardService.stats("admin@holidayplanner.vn");
        assertThat(userStats.adminView()).isFalse();
        assertThat(adminStats.adminView()).isTrue();
        assertThat(adminStats.totalTrips()).isGreaterThanOrEqualTo(userStats.totalTrips());
        assertThat(userStats.expenseByCategory()).isNotEmpty();
        assertThat(userStats.tripsByStatus()).containsKeys(TripStatus.DRAFT, TripStatus.PLANNED);
    }

    @Test
    void inactiveDestinationCannotBeSelectedForNewTrip() {
        var destination = destinationService.activeDestinations().get(0);
        destinationService.deactivate(destination.getId());
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Điểm đến đã ẩn");
        request.setDestinationId(destination.getId());
        request.setStartDate(LocalDate.of(2026, 10, 1));
        request.setEndDate(LocalDate.of(2026, 10, 2));
        request.setPeopleCount(1);
        request.setBudget(BigDecimal.ZERO);
        request.setStatus(TripStatus.DRAFT);
        assertThatThrownBy(() -> tripPlanService.create(request, "user@holidayplanner.vn"))
                .hasMessageContaining("đã bị ẩn");
    }

    @Test
    void dashboardUpcomingExcludesIneligibleTripsAndReturnsFiveInStartDateOrder() {
        UserAccount owner = userService.createSeedUser(
                "Dashboard Owner", "dashboard.owner@example.com", "owner123", Role.USER);
        Destination destination = destinationService.activeDestinations().get(0);
        LocalDate today = LocalDate.now();

        persistTrip(owner, destination, "Cancelled today", today, TripStatus.CANCELLED, false);
        persistTrip(owner, destination, "Completed today", today, TripStatus.COMPLETED, false);
        persistTrip(owner, destination, "Deleted today", today, TripStatus.PLANNED, true);
        for (int day = 7; day >= 1; day--) {
            persistTrip(owner, destination, "Upcoming " + day, today.plusDays(day),
                    TripStatus.PLANNED, false);
        }

        var userUpcoming = dashboardService.stats(owner.getEmail()).upcomingTrips();

        assertThat(userUpcoming).hasSize(5);
        assertThat(userUpcoming).extracting(TripPlan::getTitle)
                .containsExactly("Upcoming 1", "Upcoming 2", "Upcoming 3", "Upcoming 4", "Upcoming 5");
        assertThat(userUpcoming).isSortedAccordingTo(Comparator.comparing(TripPlan::getStartDate));
        assertThat(userUpcoming).noneMatch(TripPlan::isDeleted);
        assertThat(userUpcoming).noneMatch(trip -> trip.getStatus() == TripStatus.CANCELLED
                || trip.getStatus() == TripStatus.COMPLETED);

        var adminUpcoming = dashboardService.stats("admin@holidayplanner.vn").upcomingTrips();
        assertThat(adminUpcoming).hasSizeLessThanOrEqualTo(5)
                .isSortedAccordingTo(Comparator.comparing(TripPlan::getStartDate));
        assertThat(adminUpcoming).noneMatch(TripPlan::isDeleted);
        assertThat(adminUpcoming).noneMatch(trip -> trip.getStatus() == TripStatus.CANCELLED
                || trip.getStatus() == TripStatus.COMPLETED);
    }

    @Test
    void updateRetainsCurrentInactiveDestinationButRejectsSwitchToAnotherInactiveDestination() {
        var activeDestinations = destinationService.activeDestinations();
        Destination current = activeDestinations.get(0);
        Destination other = activeDestinations.get(1);
        TripPlan trip = tripPlanService.create(validTripRequest(current.getId()),
                "user@holidayplanner.vn");

        destinationService.deactivate(current.getId());
        TripPlanRequest retainCurrent = tripPlanService.toRequest(trip);
        retainCurrent.setTitle("Retain hidden destination");
        TripPlan retained = tripPlanService.update(
                trip.getId(), retainCurrent, "user@holidayplanner.vn");

        assertThat(retained.getDestination().getId()).isEqualTo(current.getId());
        assertThat(retained.getTitle()).isEqualTo("Retain hidden destination");

        destinationService.deactivate(other.getId());
        TripPlanRequest switchDestination = tripPlanService.toRequest(retained);
        switchDestination.setDestinationId(other.getId());

        assertThatThrownBy(() -> tripPlanService.update(
                trip.getId(), switchDestination, "user@holidayplanner.vn"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(retained.getDestination().getId()).isEqualTo(current.getId());
    }

    @Test
    void seedReplacesOnlyLegacyRemoteImageAndPreservesCustomImage() {
        Destination destination = destinationService.activeDestinations().get(0);
        destination.setImageUrl("https://images.unsplash.com/legacy-demo-image");
        destination.setImagePublicId("legacy-public-id");
        destinationRepository.saveAndFlush(destination);

        Destination reconciled = destinationService.createSeed(
                destination.getName(), destination.getCity(), destination.getCountry(),
                destination.getDescription(), "/images/destinations/ha-noi.svg");

        assertThat(reconciled.getImageUrl()).isEqualTo("/images/destinations/ha-noi.svg");
        assertThat(reconciled.getImagePublicId()).isNull();

        reconciled.setImageUrl("https://cdn.example.com/custom-destination.webp");
        destinationRepository.saveAndFlush(reconciled);
        Destination preserved = destinationService.createSeed(
                reconciled.getName(), reconciled.getCity(), reconciled.getCountry(),
                reconciled.getDescription(), "/images/destinations/ha-noi.svg");

        assertThat(preserved.getImageUrl()).isEqualTo("https://cdn.example.com/custom-destination.webp");
    }

    private TripPlan persistTrip(UserAccount owner, Destination destination, String title,
                                 LocalDate startDate, TripStatus status, boolean deleted) {
        TripPlan trip = new TripPlan();
        trip.setOwner(owner);
        trip.setDestination(destination);
        trip.setTitle(title);
        trip.setStartDate(startDate);
        trip.setEndDate(startDate.plusDays(2));
        trip.setPeopleCount(1);
        trip.setBudget(BigDecimal.ZERO);
        trip.setStatus(status);
        trip.setDeleted(deleted);
        return tripPlanRepository.saveAndFlush(trip);
    }

    private TripPlanRequest validTripRequest(Long destinationId) {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Inactive destination retention");
        request.setDestinationId(destinationId);
        request.setStartDate(LocalDate.of(2027, 5, 10));
        request.setEndDate(LocalDate.of(2027, 5, 12));
        request.setPeopleCount(2);
        request.setBudget(new BigDecimal("1000.00"));
        request.setStatus(TripStatus.PLANNED);
        return request;
    }
}
