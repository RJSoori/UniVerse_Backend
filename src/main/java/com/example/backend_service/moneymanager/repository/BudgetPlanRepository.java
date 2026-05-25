package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, String> {
    List<BudgetPlan> findByStudentId(Long studentId);
    void deleteByStudentId(Long studentId);
}
