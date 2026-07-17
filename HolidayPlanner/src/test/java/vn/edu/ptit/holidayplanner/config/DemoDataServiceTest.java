package vn.edu.ptit.holidayplanner.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.repository.BookingNoteRepository;
import vn.edu.ptit.holidayplanner.repository.ChecklistItemRepository;
import vn.edu.ptit.holidayplanner.repository.DestinationRepository;
import vn.edu.ptit.holidayplanner.repository.ExpenseRepository;
import vn.edu.ptit.holidayplanner.repository.ItineraryItemRepository;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;
import vn.edu.ptit.holidayplanner.repository.UserAccountRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.seed-demo-data=false",
        "spring.datasource.url=jdbc:h2:mem:demo-seed-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
@Transactional
class DemoDataServiceTest {
    @Autowired private DemoDataService demoDataService;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private TripPlanRepository tripPlanRepository;
    @Autowired private ItineraryItemRepository itineraryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ChecklistItemRepository checklistRepository;
    @Autowired private BookingNoteRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Clock clock;

    @Test
    void seedCreatesCompleteDatasetAndRepeatedRunsDoNotDuplicateAnything() {
        assertThat(userRepository.count()).isZero();
        assertThat(destinationRepository.count()).isZero();
        assertThat(tripPlanRepository.count()).isZero();

        demoDataService.seed();

        assertSeedAccountsUseBCrypt();
        assertSeedDestinationsAreComplete();
        assertSeedTripsAreComplete();
        DatabaseCounts firstRun = counts();

        demoDataService.seed();

        assertThat(counts()).isEqualTo(firstRun);
        assertThat(firstRun).isEqualTo(new DatabaseCounts(3, 5, 5, 15, 15, 20, 10));
    }

    @Test
    void seedRepairsMissingChildWithoutDuplicatingExistingChildren() {
        demoDataService.seed();
        TripPlan trip = tripPlanRepository.findAll().get(0);
        var itinerary = itineraryRepository.findByTripPlanOrderByDayNoAscFromTimeAsc(trip);
        itineraryRepository.delete(itinerary.get(0));
        itineraryRepository.flush();

        assertThat(itineraryRepository.count()).isEqualTo(14);

        demoDataService.seed();

        assertThat(itineraryRepository.count()).isEqualTo(15);
        assertThat(counts()).isEqualTo(new DatabaseCounts(3, 5, 5, 15, 15, 20, 10));
    }

    private void assertSeedAccountsUseBCrypt() {
        UserAccount admin = requireUser(DemoDataService.ADMIN_EMAIL);
        UserAccount demoUser = requireUser(DemoDataService.DEMO_USER_EMAIL);
        UserAccount traveler = requireUser(DemoDataService.TRAVELER_EMAIL);

        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(demoUser.getRole()).isEqualTo(Role.USER);
        assertThat(traveler.getRole()).isEqualTo(Role.USER);
        assertThat(admin.getPassword()).startsWith("$2");
        assertThat(demoUser.getPassword()).startsWith("$2");
        assertThat(traveler.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("user1234", demoUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("traveler123", traveler.getPassword())).isTrue();
    }

    private void assertSeedDestinationsAreComplete() {
        List<Destination> destinations = destinationRepository.findAllByOrderByNameAsc();
        assertThat(destinations).hasSize(5);
        assertThat(destinations).extracting(Destination::getName)
                .containsExactlyInAnyOrder("Hà Nội", "Đà Nẵng", "Đà Lạt", "Phú Quốc", "Tokyo");
        assertThat(destinations).allSatisfy(destination -> {
            assertThat(destination.getCity()).isNotBlank();
            assertThat(destination.getCountry()).isNotBlank();
            assertThat(destination.getDescription()).isNotBlank();
            assertThat(destination.getImageUrl()).startsWith("/images/");
            assertThat(destination.isActive()).isTrue();
        });
    }

    private void assertSeedTripsAreComplete() {
        UserAccount demoUser = requireUser(DemoDataService.DEMO_USER_EMAIL);
        List<TripPlan> trips = tripPlanRepository
                .findAllByOwnerAndDeletedFalseOrderByStartDateDesc(demoUser);
        LocalDate today = LocalDate.now(clock);

        assertThat(trips).hasSize(5);
        assertThat(trips).extracting(TripPlan::getTitle).containsExactlyInAnyOrder(
                "Chuyến Hà Nội cuối tuần",
                "Nghỉ dưỡng Đà Nẵng",
                "Săn mây Đà Lạt",
                "Khám phá Phú Quốc",
                "Tokyo mùa thu");
        assertThat(trips).extracting(TripPlan::getStatus)
                .containsExactlyInAnyOrder(TripStatus.values());
        assertThat(trips).anySatisfy(trip -> {
            assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
            assertThat(trip.getEndDate()).isBefore(today);
        });
        assertThat(trips).anySatisfy(trip -> assertThat(trip.getStartDate()).isAfter(today));
        assertThat(trips).anySatisfy(trip -> assertThat(trip.getStatus()).isEqualTo(TripStatus.DRAFT));
        assertThat(trips).anySatisfy(trip -> {
            BigDecimal total = expenseRepository.sumByTrip(trip);
            assertThat(total).isGreaterThan(trip.getBudget());
        });

        assertThat(trips).allSatisfy(trip -> {
            assertThat(trip.getDestination()).isNotNull();
            assertThat(trip.getStartDate()).isBeforeOrEqualTo(trip.getEndDate());
            assertThat(trip.getPeopleCount()).isPositive();
            assertThat(trip.getBudget()).isNotNegative();
            assertThat(trip.getNotes()).isNotBlank();
            assertThat(itineraryRepository.findByTripPlanOrderByDayNoAscFromTimeAsc(trip)).hasSize(3);
            assertThat(expenseRepository.findByTripPlanOrderBySpentDateDesc(trip)).hasSize(3);
            assertThat(checklistRepository.findByTripPlanOrderByDoneAscDueDateAsc(trip))
                    .hasSize(4)
                    .anyMatch(item -> item.isDone());
            assertThat(bookingRepository.findByTripPlanOrderByIdDesc(trip)).hasSize(2);
        });
    }

    private UserAccount requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private DatabaseCounts counts() {
        return new DatabaseCounts(
                userRepository.count(),
                destinationRepository.count(),
                tripPlanRepository.count(),
                itineraryRepository.count(),
                expenseRepository.count(),
                checklistRepository.count(),
                bookingRepository.count());
    }

    private record DatabaseCounts(
            long users,
            long destinations,
            long trips,
            long itinerary,
            long expenses,
            long checklist,
            long bookings) {
    }
}
