package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {
    List<TransactionRecord> findByWalletIdOrderByDateDescTimeDesc(String walletId);
    List<TransactionRecord> findByStudentId(Long studentId);
    Optional<TransactionRecord> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
}
