package com.securebank.bms.repository;

import com.securebank.bms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(a.action) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.entityReference) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<AuditLog> search(@Param("q") String q, Pageable pageable);
}
