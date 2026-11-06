package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    @Query("""
            select u from UserAccount u
            where u.role = :role
              and (:active is null or u.active = :active)
              and (cast(:search as string) is null
                   or lower(u.fullName) like lower(concat('%', cast(:search as string), '%'))
                   or lower(u.username) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<UserAccount> searchByRole(
            @Param("role") UserRole role,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    long countByRoleAndActiveTrue(UserRole role);

    long countByRole(UserRole role);
}
