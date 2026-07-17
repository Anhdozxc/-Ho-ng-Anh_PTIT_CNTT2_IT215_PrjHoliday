package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.holidayplanner.domain.ChecklistItem;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem,Long> {
    List<ChecklistItem> findByTripPlanOrderByDoneAscDueDateAsc(TripPlan tripPlan);
    Optional<ChecklistItem> findByIdAndTripPlan(Long id, TripPlan tripPlan);

    @Query("select count(c) from ChecklistItem c where c.tripPlan.owner=:owner and c.tripPlan.deleted=false and c.done=false")
    long countOpenByOwner(@Param("owner") UserAccount owner);

    @Query("select count(c) from ChecklistItem c where c.tripPlan.deleted=false and c.done=false")
    long countOpenAll();
}
