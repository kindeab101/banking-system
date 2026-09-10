package com.securebank.bms.repository;

import com.securebank.bms.entity.Account;
import com.securebank.bms.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerIdOrderByOpenedAtAsc(Long customerId);

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    long countByStatus(AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT a FROM Account a
            WHERE (:q IS NULL OR :q = ''
                OR a.accountNumber LIKE CONCAT('%', :q, '%')
                OR LOWER(a.customer.lastName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<Account> search(@Param("q") String q, @Param("status") AccountStatus status, Pageable pageable);
}
