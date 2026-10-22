package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Entity
@Table(name = "money_wallets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "name"})
})
public class Wallet extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be in YYYY-MM-DD format")
    @Column(nullable = false, length = 10)
    private String createdDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletType type = WalletType.CASH;

    @Column(nullable = false)
    private boolean includeInTotal = true;

    public Wallet() {
    }

    public Wallet(String name, BigDecimal balance, String createdDate, WalletType type, boolean includeInTotal) {
        this.name = name;
        this.balance = balance;
        this.createdDate = createdDate;
        this.type = type;
        this.includeInTotal = includeInTotal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public WalletType getType() {
        return type;
    }

    public void setType(WalletType type) {
        this.type = type;
    }

    public boolean isIncludeInTotal() {
        return includeInTotal;
    }

    public void setIncludeInTotal(boolean includeInTotal) {
        this.includeInTotal = includeInTotal;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
