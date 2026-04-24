package com.example.backend_service.moneymanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "money_settings")
public class MoneyManagerSettings extends BaseEntity {

    @Column(nullable = false)
    private boolean firstTimeSetupCompleted;

    @Column(nullable = false, length = 16)
    private String currency = "LKR";

    @Column(length = 32)
    private String theme;

    public MoneyManagerSettings() {
        setId("default");
    }

    public MoneyManagerSettings(boolean firstTimeSetupCompleted, String currency, String theme) {
        setId("default");
        this.firstTimeSetupCompleted = firstTimeSetupCompleted;
        this.currency = currency;
        this.theme = theme;
    }

    public boolean isFirstTimeSetupCompleted() {
        return firstTimeSetupCompleted;
    }

    public void setFirstTimeSetupCompleted(boolean firstTimeSetupCompleted) {
        this.firstTimeSetupCompleted = firstTimeSetupCompleted;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
