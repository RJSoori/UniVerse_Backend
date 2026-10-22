package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, String> {
    List<CategoryBudget> findByStudentId(Long studentId);
    Optional<CategoryBudget> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
    Optional<CategoryBudget> findByStudentIdAndCategoryAndMonth(Long studentId, String category, String month);
}
