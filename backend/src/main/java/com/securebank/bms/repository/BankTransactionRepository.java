package com.securebank.bms.repository;

import com.securebank.bms.entity.BankTransaction;
import com.securebank.bms.entity.TransactionStatus;
import com.securebank.bms.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findByReference(String reference);

    Optional<BankTransaction> findByCreatedByIdAndIdempotencyKey(Long userId, String idempotencyKey);

    boolean existsByReference(String reference);

    @Query("""
            SELECT t FROM BankTransaction t
            WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
              AND t.createdAt >= :from AND t.createdAt < :to
            ORDER BY t.createdAt ASC
            """)
    List<BankTransaction> findForStatement(@Param("accountId") Long accountId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);

    @Query("""
            SELECT t FROM BankTransaction t
            WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
            ORDER BY t.createdAt DESC
            """)
    Page<BankTransaction> findForAccount(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT t FROM BankTransaction t
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(t.reference) LIKE LOWER(CONCAT('%', :q, '%'))
                OR t.sourceAccount.accountNumber LIKE CONCAT('%', :q, '%')
                OR t.destinationAccount.accountNumber LIKE CONCAT('%', :q, '%'))
              AND (:status IS NULL OR t.status = :status)
              AND (:type IS NULL OR t.transactionType = :type)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt < :to)
            """)
    Page<BankTransaction> search(@Param("q") String q,
                                 @Param("status") TransactionStatus status,
                                 @Param("type") TransactionType type,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to,
                                 Pageable pageable);

    long countByStatus(TransactionStatus status);

    long countByCreatedAtGreaterThanEqual(Instant from);

    long countByStatusAndCreatedAtGreaterThanEqual(TransactionStatus status, Instant from);
}
