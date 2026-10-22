package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {
    List<TransactionRecord> findByStudentId(Long studentId);
    Optional<TransactionRecord> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
    void deleteByWalletId(String walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionRecord t WHERE t.studentId = :studentId AND t.category = :category AND t.date LIKE :monthPrefix%")
    BigDecimal sumExpensesByStudentIdAndCategoryAndMonth(
            @Param("studentId") Long studentId,
            @Param("category") String category,
            @Param("monthPrefix") String monthPrefix);
}
