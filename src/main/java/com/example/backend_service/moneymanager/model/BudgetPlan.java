package com.example.backend_service.moneymanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "money_budgets")
public class BudgetPlan extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyBudget = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AllocationMode allocationMode = AllocationMode.RECOMMENDED;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal needs = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal wants = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal savings = BigDecimal.ZERO;

    @Column(name = "budget_month", nullable = false, length = 7)
    private String month;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(nullable = false, length = 19)
    private String lastUpdated;

    public BudgetPlan() {
    }

    public BudgetPlan(BigDecimal monthlyIncome, BigDecimal monthlyBudget, AllocationMode allocationMode,
            BigDecimal needs, BigDecimal wants, BigDecimal savings, String month, BigDecimal totalSpent,
            String lastUpdated) {
        this.monthlyIncome = monthlyIncome;
        this.monthlyBudget = monthlyBudget;
        this.allocationMode = allocationMode;
        this.needs = needs;
        this.wants = wants;
        this.savings = savings;
        this.month = month;
        this.totalSpent = totalSpent;
        this.lastUpdated = lastUpdated;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public AllocationMode getAllocationMode() {
        return allocationMode;
    }

    public void setAllocationMode(AllocationMode allocationMode) {
        this.allocationMode = allocationMode;
    }

    public BigDecimal getNeeds() {
        return needs;
    }

    public void setNeeds(BigDecimal needs) {
        this.needs = needs;
    }

    public BigDecimal getWants() {
        return wants;
    }

    public void setWants(BigDecimal wants) {
        this.wants = wants;
    }

    public BigDecimal getSavings() {
        return savings;
    }

    public void setSavings(BigDecimal savings) {
        this.savings = savings;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
