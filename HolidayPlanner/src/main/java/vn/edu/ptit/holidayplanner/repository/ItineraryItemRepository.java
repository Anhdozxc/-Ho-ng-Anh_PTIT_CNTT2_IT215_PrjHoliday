package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.holidayplanner.domain.ItineraryItem;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import java.util.List;
import java.util.Optional;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem,Long> {
    List<ItineraryItem> findByTripPlanOrderByDayNoAscFromTimeAsc(TripPlan tripPlan);
    Optional<ItineraryItem> findByIdAndTripPlan(Long id, TripPlan tripPlan);
}
