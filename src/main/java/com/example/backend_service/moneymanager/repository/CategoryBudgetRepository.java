package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, String> {
    List<CategoryBudget> findByStudentId(Long studentId);
    void deleteByStudentId(Long studentId);
}
