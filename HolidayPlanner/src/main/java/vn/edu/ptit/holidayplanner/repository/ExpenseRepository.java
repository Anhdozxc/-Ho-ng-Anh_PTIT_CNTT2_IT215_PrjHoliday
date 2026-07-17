package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.holidayplanner.domain.Expense;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense,Long> {
    List<Expense> findByTripPlanOrderBySpentDateDesc(TripPlan tripPlan);
    List<Expense> findByTripPlanAndCategoryOrderBySpentDateDesc(TripPlan tripPlan, ExpenseCategory category);
    Optional<Expense> findByIdAndTripPlan(Long id, TripPlan tripPlan);

    @Query("select coalesce(sum(e.amount),0) from Expense e where e.tripPlan=:trip")
    BigDecimal sumByTrip(@Param("trip") TripPlan trip);

    @Query("select coalesce(sum(e.amount),0) from Expense e where e.tripPlan.owner=:owner and e.tripPlan.deleted=false")
    BigDecimal sumByOwner(@Param("owner") UserAccount owner);

    @Query("select coalesce(sum(e.amount),0) from Expense e where e.tripPlan.deleted=false")
    BigDecimal sumAllActiveTrips();

    @Query("select e.category, coalesce(sum(e.amount),0) from Expense e " +
            "where e.tripPlan.owner=:owner and e.tripPlan.deleted=false group by e.category")
    List<Object[]> sumByCategoryForOwner(@Param("owner") UserAccount owner);

    @Query("select e.category, coalesce(sum(e.amount),0) from Expense e " +
            "where e.tripPlan.deleted=false group by e.category")
    List<Object[]> sumByCategoryAll();
}
