package vn.edu.ptit.holidayplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleAndStatus(Role role, UserStatus status);

    @Query("select u from UserAccount u where " +
            "(:query is null or lower(u.fullName) like lower(concat('%',:query,'%')) " +
            "or lower(u.email) like lower(concat('%',:query,'%'))) " +
            "and (:role is null or u.role=:role) and (:status is null or u.status=:status)")
    Page<UserAccount> search(@Param("query") String query,
                             @Param("role") Role role,
                             @Param("status") UserStatus status,
                             Pageable pageable);
}
