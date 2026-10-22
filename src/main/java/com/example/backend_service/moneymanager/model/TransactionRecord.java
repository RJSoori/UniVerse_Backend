package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
@Table(name = "money_transactions")
public class TransactionRecord extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String category;

    @NotBlank
    @Column(nullable = false, length = 36)
    private String walletId;

    @Column(length = 255)
    private String description;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be in YYYY-MM-DD format")
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
