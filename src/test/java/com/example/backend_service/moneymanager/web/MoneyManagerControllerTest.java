package com.example.backend_service.moneymanager.web;

import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import com.example.backend_service.moneymanager.model.Wallet;
import com.example.backend_service.moneymanager.model.WalletType;
import com.example.backend_service.moneymanager.repository.BudgetPlanRepository;
import com.example.backend_service.moneymanager.repository.CategoryBudgetRepository;
import com.example.backend_service.moneymanager.repository.MoneyManagerSettingsRepository;
import com.example.backend_service.moneymanager.repository.RecurringExpenseRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import com.example.backend_service.moneymanager.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyManagerControllerTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RecurringExpenseRepository recurringExpenseRepository;
    @Mock private CategoryBudgetRepository categoryBudgetRepository;
    @Mock private BudgetPlanRepository budgetPlanRepository;
    @Mock private MoneyManagerSettingsRepository settingsRepository;

    @InjectMocks
    private MoneyManagerController controller;

    @Test
    void createWallet_setsStudentIdAndClearsIncomingId() {
        Wallet wallet = new Wallet("Main", BigDecimal.TEN, "2026-05-26", WalletType.CASH, true);
        wallet.setStudentId(999L);
        wallet.setId("existing");
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet saved = controller.createWallet(42L, wallet);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getStudentId()).isEqualTo(42L);
        verify(walletRepository).save(wallet);
    }

    @Test
    void getWallets_returnsStudentWallets() {
        Wallet wallet = new Wallet("Main", BigDecimal.TEN, "2026-05-26", WalletType.CASH, true);
        when(walletRepository.findByStudentId(42L)).thenReturn(List.of(wallet));

        List<Wallet> result = controller.getWallets(42L);

        assertThat(result).containsExactly(wallet);
    }

    @Test
    void getSettings_createsDefaultSettingsWhenMissing() {
        when(settingsRepository.findByStudentId(42L)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(MoneyManagerSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        MoneyManagerSettings settings = controller.getSettings(42L);

        assertThat(settings.getStudentId()).isEqualTo(42L);
        assertThat(settings.getCurrency()).isEqualTo("LKR");
        verify(settingsRepository).save(settings);
    }

    @Test
    void saveSettings_updatesExistingRow() {
        MoneyManagerSettings existing = new MoneyManagerSettings(true, "LKR", "light");
        existing.setStudentId(42L);
        when(settingsRepository.findByStudentId(42L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(MoneyManagerSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        MoneyManagerSettings incoming = new MoneyManagerSettings(false, "USD", "dark");
        MoneyManagerSettings result = controller.saveSettings(42L, incoming);

        assertThat(result.getStudentId()).isEqualTo(42L);
        assertThat(result.isFirstTimeSetupCompleted()).isFalse();
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getTheme()).isEqualTo("dark");
    }
}