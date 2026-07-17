package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripPlanRepository extends JpaRepository<TripPlan,Long> {
    List<TripPlan> findAllByOwnerAndDeletedFalseOrderByStartDateDesc(UserAccount owner);
    Optional<TripPlan> findByIdAndOwnerAndDeletedFalse(Long id, UserAccount owner);
    @EntityGraph(attributePaths = {"destination", "owner"})
    Optional<TripPlan> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"destination", "owner"})
    List<TripPlan> findByOwnerAndDeletedFalse(UserAccount owner, Sort sort);

    @EntityGraph(attributePaths = {"destination", "owner"})
    List<TripPlan> findByDeletedFalse(Sort sort);

    @Query("""
            select t from TripPlan t
            where t.owner = :owner
              and t.deleted = false
              and (:status is null or t.status = :status)
              and (:query is null
                   or lower(t.title) like concat('%', lower(:query), '%')
                   or lower(t.destination.name) like concat('%', lower(:query), '%')
                   or lower(t.destination.country) like concat('%', lower(:query), '%'))
            """)
    @EntityGraph(attributePaths = {"destination", "owner"})
    Page<TripPlan> searchOwned(@Param("owner") UserAccount owner,
                               @Param("query") String query,
                               @Param("status") TripStatus status,
                               Pageable pageable);

    @Query("""
            select t from TripPlan t
            where t.deleted = false
              and (:status is null or t.status = :status)
              and (:query is null
                   or lower(t.title) like concat('%', lower(:query), '%')
                   or lower(t.destination.name) like concat('%', lower(:query), '%')
                   or lower(t.destination.country) like concat('%', lower(:query), '%'))
            """)
    @EntityGraph(attributePaths = {"destination", "owner"})
    Page<TripPlan> searchAll(@Param("query") String query,
                             @Param("status") TripStatus status,
                             Pageable pageable);

    long countByOwnerAndDeletedFalse(UserAccount owner);
    long countByDeletedFalse();

    @EntityGraph(attributePaths = {"destination", "owner"})
    List<TripPlan> findTop5ByOwnerAndDeletedFalseAndStartDateGreaterThanEqualAndStatusNotInOrderByStartDateAscIdAsc(
            UserAccount owner, LocalDate today, List<TripStatus> excludedStatuses);

    @EntityGraph(attributePaths = {"destination", "owner"})
    List<TripPlan> findTop5ByDeletedFalseAndStartDateGreaterThanEqualAndStatusNotInOrderByStartDateAscIdAsc(
            LocalDate today, List<TripStatus> excludedStatuses);

    @Query("select t.status, count(t) from TripPlan t where t.owner=:owner and t.deleted=false group by t.status")
    List<Object[]> countByStatusForOwner(@Param("owner") UserAccount owner);

    @Query("select t.status, count(t) from TripPlan t where t.deleted=false group by t.status")
    List<Object[]> countByStatusAll();
}
