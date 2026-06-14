package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
@Table(name = "money_category_budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "category", "budget_month"})
})
public class CategoryBudget extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String category;

    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be in YYYY-MM format")
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
