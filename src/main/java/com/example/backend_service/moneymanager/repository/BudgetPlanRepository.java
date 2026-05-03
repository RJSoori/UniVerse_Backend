package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, String> {
    Optional<BudgetPlan> findByMonth(String month);
    List<BudgetPlan> findByStudentId(Long studentId);
    Optional<BudgetPlan> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
}
