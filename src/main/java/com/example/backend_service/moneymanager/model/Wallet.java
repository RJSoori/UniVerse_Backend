package com.example.backend_service.moneymanager.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "money_wallets")
public class Wallet extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

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
