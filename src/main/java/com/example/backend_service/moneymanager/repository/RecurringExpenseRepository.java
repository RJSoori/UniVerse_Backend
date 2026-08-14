package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, String> {
    List<RecurringExpense> findByStudentId(Long studentId);
    Optional<RecurringExpense> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
    void deleteByWalletId(String walletId);
}
