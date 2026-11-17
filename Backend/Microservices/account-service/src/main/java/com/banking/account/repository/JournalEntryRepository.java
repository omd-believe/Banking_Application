package com.banking.account.repository;

import com.banking.account.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    boolean existsByReferenceId(String referenceId);
}