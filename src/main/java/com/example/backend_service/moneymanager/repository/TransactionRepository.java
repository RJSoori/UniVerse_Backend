package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {
    List<TransactionRecord> findByWalletIdOrderByDateDescTimeDesc(String walletId);
}
