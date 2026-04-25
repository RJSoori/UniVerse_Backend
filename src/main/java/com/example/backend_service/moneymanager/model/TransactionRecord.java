package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "money_transactions")
public class TransactionRecord extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 36)
    private String walletId;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 10)
    private String date;

    @Column(length = 8)
    private String time;

    @Column(nullable = false)
    private boolean isRecurring;

    @Column(length = 36)
    private String recurringId;

    public TransactionRecord() {
    }

    public TransactionRecord(TransactionType type, BigDecimal amount, String category, String walletId,
            String description, String date, String time, boolean isRecurring, String recurringId) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.walletId = walletId;
        this.description = description;
        this.date = date;
        this.time = time;
        this.isRecurring = isRecurring;
        this.recurringId = recurringId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public String getRecurringId() {
        return recurringId;
    }

    public void setRecurringId(String recurringId) {
        this.recurringId = recurringId;
    }
}
