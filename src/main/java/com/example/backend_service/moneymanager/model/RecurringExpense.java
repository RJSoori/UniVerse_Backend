package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "money_recurring_expenses")
public class RecurringExpense extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 36)
    private String walletId;

    @Column(nullable = false, length = 16)
    private String frequency = "monthly";

    @Column(nullable = false, length = 10)
    private String startDate;

    @Column(nullable = false, length = 10)
    private String endDate;

    @Column(length = 10)
    private String lastProcessedDate;

    public RecurringExpense() {
    }

    public RecurringExpense(String title, BigDecimal amount, String category, String walletId, String frequency,
            String startDate, String endDate, String lastProcessedDate) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.walletId = walletId;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lastProcessedDate = lastProcessedDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLastProcessedDate() {
        return lastProcessedDate;
    }

    public void setLastProcessedDate(String lastProcessedDate) {
        this.lastProcessedDate = lastProcessedDate;
    }
}
