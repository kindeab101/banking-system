package com.securebank.bms.repository;

import com.securebank.bms.entity.Customer;
import com.securebank.bms.entity.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.user.email) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Customer> search(@Param("q") String q, @Param("status") CustomerStatus status, Pageable pageable);
}
