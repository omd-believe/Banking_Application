package com.banking.account.repository;

import com.banking.account.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByOrderByIdDesc();
    List<AuditLog> findAllByOrderByIdAsc();
}