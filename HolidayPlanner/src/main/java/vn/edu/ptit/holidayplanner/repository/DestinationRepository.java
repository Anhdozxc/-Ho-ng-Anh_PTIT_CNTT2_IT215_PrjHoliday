package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.holidayplanner.domain.Destination;
import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination,Long> {
    List<Destination> findAllByOrderByNameAsc();
    List<Destination> findByActiveTrueOrderByNameAsc();

    @Query("select d from Destination d where " +
            "(:query is null or lower(d.name) like lower(concat('%',:query,'%')) " +
            "or lower(d.city) like lower(concat('%',:query,'%')) " +
            "or lower(d.country) like lower(concat('%',:query,'%'))) " +
            "and (:active is null or d.active=:active)")
    Page<Destination> search(@Param("query") String query,
                             @Param("active") Boolean active,
                             Pageable pageable);
}
