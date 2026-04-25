package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "money_category_budgets")
public class CategoryBudget extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "budget_month", nullable = false, length = 7)
    private String month;

    public CategoryBudget() {
    }

    public CategoryBudget(String category, BigDecimal limitAmount, String month) {
        this.category = category;
        this.limitAmount = limitAmount;
        this.month = month;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
