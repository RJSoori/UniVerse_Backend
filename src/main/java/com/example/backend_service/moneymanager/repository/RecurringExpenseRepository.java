package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, String> {
    List<RecurringExpense> findByWalletId(String walletId);
}
