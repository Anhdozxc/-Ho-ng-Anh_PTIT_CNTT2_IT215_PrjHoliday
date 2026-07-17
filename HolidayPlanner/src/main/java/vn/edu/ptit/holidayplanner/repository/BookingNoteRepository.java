package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.holidayplanner.domain.BookingNote;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import java.util.List;
import java.util.Optional;

public interface BookingNoteRepository extends JpaRepository<BookingNote,Long> {
    List<BookingNote> findByTripPlanOrderByIdDesc(TripPlan tripPlan);
    Optional<BookingNote> findByIdAndTripPlan(Long id, TripPlan tripPlan);
}
