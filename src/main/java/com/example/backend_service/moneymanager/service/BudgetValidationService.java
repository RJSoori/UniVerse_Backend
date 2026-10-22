package com.example.backend_service.moneymanager.service;

import com.example.backend_service.moneymanager.repository.CategoryBudgetRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BudgetValidationService {

    private final CategoryBudgetRepository categoryBudgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetValidationService(CategoryBudgetRepository categoryBudgetRepository,
                                   TransactionRepository transactionRepository) {
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.transactionRepository = transactionRepository;
    }

    public boolean wouldExceedBudget(Long studentId, String category, BigDecimal amount, String month) {
        return categoryBudgetRepository
                .findByStudentIdAndCategoryAndMonth(studentId, category, month)
                .map(budget -> {
                    BigDecimal spent = transactionRepository.sumExpensesByStudentIdAndCategoryAndMonth(
                            studentId, category, month);
                    return spent.add(amount).compareTo(budget.getLimitAmount()) > 0;
                })
                .orElse(false);
    }
}
