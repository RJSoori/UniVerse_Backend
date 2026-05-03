package com.example.backend_service.moneymanager.web;

import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.moneymanager.model.BudgetPlan;
import com.example.backend_service.moneymanager.model.CategoryBudget;
import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import com.example.backend_service.moneymanager.model.RecurringExpense;
import com.example.backend_service.moneymanager.model.TransactionRecord;
import com.example.backend_service.moneymanager.model.Wallet;
import com.example.backend_service.moneymanager.repository.BudgetPlanRepository;
import com.example.backend_service.moneymanager.repository.CategoryBudgetRepository;
import com.example.backend_service.moneymanager.repository.MoneyManagerSettingsRepository;
import com.example.backend_service.moneymanager.repository.RecurringExpenseRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import com.example.backend_service.moneymanager.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/money-manager")
public class MoneyManagerController {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final MoneyManagerSettingsRepository settingsRepository;

    public MoneyManagerController(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            CategoryBudgetRepository categoryBudgetRepository,
            BudgetPlanRepository budgetPlanRepository,
            MoneyManagerSettingsRepository settingsRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.budgetPlanRepository = budgetPlanRepository;
        this.settingsRepository = settingsRepository;
    }

    @GetMapping("/wallets")
    public List<Wallet> getWallets(@AuthenticationPrincipal Long authStudentId) {
        return walletRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public Wallet createWallet(@AuthenticationPrincipal Long authStudentId, @RequestBody Wallet wallet) {
        wallet.setId(null);
        wallet.setStudentId(authStudentId);
        return walletRepository.save(wallet);
    }

    @Transactional
    @PutMapping("/wallets/bulk")
    public List<Wallet> replaceWallets(@AuthenticationPrincipal Long authStudentId, @RequestBody List<Wallet> wallets) {
        wallets.forEach(wallet -> ensureWalletIdIsNewOrOwned(wallet.getId(), authStudentId));
        walletRepository.deleteByStudentId(authStudentId);
        walletRepository.flush();
        wallets.forEach(wallet -> wallet.setStudentId(authStudentId));
        return walletRepository.saveAll(wallets);
    }

    @PutMapping("/wallets/{id}")
    public Wallet updateWallet(@AuthenticationPrincipal Long authStudentId, @PathVariable String id,
            @RequestBody Wallet wallet) {
        walletRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        wallet.setId(id);
        wallet.setStudentId(authStudentId);
        return walletRepository.save(wallet);
    }

    @DeleteMapping("/wallets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallet(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        walletRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        walletRepository.deleteById(id);
    }

    @GetMapping("/transactions")
    public List<TransactionRecord> getTransactions(@AuthenticationPrincipal Long authStudentId) {
        return transactionRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionRecord createTransaction(@AuthenticationPrincipal Long authStudentId,
            @RequestBody TransactionRecord transactionRecord) {
        transactionRecord.setId(null);
        ensureWalletBelongsToStudent(transactionRecord.getWalletId(), authStudentId);
        transactionRecord.setStudentId(authStudentId);
        return transactionRepository.save(transactionRecord);
    }

    @Transactional
    @PutMapping("/transactions/bulk")
    public List<TransactionRecord> replaceTransactions(@AuthenticationPrincipal Long authStudentId,
            @RequestBody List<TransactionRecord> transactions) {
        transactions.forEach(transaction -> {
            ensureTransactionIdIsNewOrOwned(transaction.getId(), authStudentId);
            ensureWalletBelongsToStudent(transaction.getWalletId(), authStudentId);
        });
        transactionRepository.deleteByStudentId(authStudentId);
        transactionRepository.flush();
        transactions.forEach(transaction -> transaction.setStudentId(authStudentId));
        return transactionRepository.saveAll(transactions);
    }

    @GetMapping("/recurring-expenses")
    public List<RecurringExpense> getRecurringExpenses(@AuthenticationPrincipal Long authStudentId) {
        return recurringExpenseRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/recurring-expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpense createRecurringExpense(@AuthenticationPrincipal Long authStudentId,
            @RequestBody RecurringExpense recurringExpense) {
        recurringExpense.setId(null);
        ensureWalletBelongsToStudent(recurringExpense.getWalletId(), authStudentId);
        recurringExpense.setStudentId(authStudentId);
        return recurringExpenseRepository.save(recurringExpense);
    }

    @Transactional
    @PutMapping("/recurring-expenses/bulk")
    public List<RecurringExpense> replaceRecurringExpenses(@AuthenticationPrincipal Long authStudentId,
            @RequestBody List<RecurringExpense> recurringExpenses) {
        recurringExpenses.forEach(recurringExpense -> {
            ensureRecurringExpenseIdIsNewOrOwned(recurringExpense.getId(), authStudentId);
            ensureWalletBelongsToStudent(recurringExpense.getWalletId(), authStudentId);
        });
        recurringExpenseRepository.deleteByStudentId(authStudentId);
        recurringExpenseRepository.flush();
        recurringExpenses.forEach(recurringExpense -> recurringExpense.setStudentId(authStudentId));
        return recurringExpenseRepository.saveAll(recurringExpenses);
    }

    @GetMapping("/category-budgets")
    public List<CategoryBudget> getCategoryBudgets(@AuthenticationPrincipal Long authStudentId) {
        return categoryBudgetRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/category-budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBudget createCategoryBudget(@AuthenticationPrincipal Long authStudentId,
            @RequestBody CategoryBudget categoryBudget) {
        categoryBudget.setId(null);
        categoryBudget.setStudentId(authStudentId);
        return categoryBudgetRepository.save(categoryBudget);
    }

    @Transactional
    @PutMapping("/category-budgets/bulk")
    public List<CategoryBudget> replaceCategoryBudgets(@AuthenticationPrincipal Long authStudentId,
            @RequestBody List<CategoryBudget> categoryBudgets) {
        categoryBudgets.forEach(categoryBudget -> ensureCategoryBudgetIdIsNewOrOwned(categoryBudget.getId(), authStudentId));
        categoryBudgetRepository.deleteByStudentId(authStudentId);
        categoryBudgetRepository.flush();
        categoryBudgets.forEach(categoryBudget -> categoryBudget.setStudentId(authStudentId));
        return categoryBudgetRepository.saveAll(categoryBudgets);
    }

    @GetMapping("/budgets/current")
    public List<BudgetPlan> getBudgets(@AuthenticationPrincipal Long authStudentId) {
        return budgetPlanRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetPlan saveBudget(@AuthenticationPrincipal Long authStudentId, @RequestBody BudgetPlan budgetPlan) {
        budgetPlan.setId(null);
        budgetPlan.setStudentId(authStudentId);
        return budgetPlanRepository.save(budgetPlan);
    }

    @Transactional
    @PutMapping("/budgets/bulk")
    public List<BudgetPlan> replaceBudgets(@AuthenticationPrincipal Long authStudentId,
            @RequestBody List<BudgetPlan> budgets) {
        budgets.forEach(budget -> ensureBudgetIdIsNewOrOwned(budget.getId(), authStudentId));
        budgetPlanRepository.deleteByStudentId(authStudentId);
        budgetPlanRepository.flush();
        budgets.forEach(budget -> budget.setStudentId(authStudentId));
        return budgetPlanRepository.saveAll(budgets);
    }

    @GetMapping("/settings")
    public MoneyManagerSettings getSettings(@AuthenticationPrincipal Long authStudentId) {
        return settingsRepository.findByStudentId(authStudentId)
                .orElseGet(() -> {
                    MoneyManagerSettings settings = new MoneyManagerSettings(false, "LKR", null);
                    settings.setStudentId(authStudentId);
                    return settingsRepository.save(settings);
                });
    }

    @PutMapping("/settings")
    public MoneyManagerSettings saveSettings(@AuthenticationPrincipal Long authStudentId,
            @RequestBody MoneyManagerSettings incoming) {
        MoneyManagerSettings settings = settingsRepository.findByStudentId(authStudentId)
                .orElseGet(() -> {
                    MoneyManagerSettings fresh = new MoneyManagerSettings(false, "LKR", null);
                    fresh.setStudentId(authStudentId);
                    return fresh;
                });
        settings.setStudentId(authStudentId);
        settings.setFirstTimeSetupCompleted(incoming.isFirstTimeSetupCompleted());
        if (incoming.getCurrency() != null) {
            settings.setCurrency(incoming.getCurrency());
        }
        if (incoming.getTheme() != null) {
            settings.setTheme(incoming.getTheme());
        }
        return settingsRepository.save(settings);
    }

    private void ensureWalletBelongsToStudent(String walletId, Long authStudentId) {
        if (walletId == null || walletId.isBlank()) {
            return;
        }
        walletRepository.findByIdAndStudentId(walletId, authStudentId)
                .orElseThrow(NotFoundException::new);
    }

    private void ensureWalletIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        walletRepository.findById(id)
                .filter(wallet -> !wallet.getStudentId().equals(authStudentId))
                .ifPresent(wallet -> {
                    throw new NotFoundException();
                });
    }

    private void ensureTransactionIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        transactionRepository.findById(id)
                .filter(transaction -> !transaction.getStudentId().equals(authStudentId))
                .ifPresent(transaction -> {
                    throw new NotFoundException();
                });
    }

    private void ensureRecurringExpenseIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        recurringExpenseRepository.findById(id)
                .filter(recurringExpense -> !recurringExpense.getStudentId().equals(authStudentId))
                .ifPresent(recurringExpense -> {
                    throw new NotFoundException();
                });
    }

    private void ensureCategoryBudgetIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        categoryBudgetRepository.findById(id)
                .filter(categoryBudget -> !categoryBudget.getStudentId().equals(authStudentId))
                .ifPresent(categoryBudget -> {
                    throw new NotFoundException();
                });
    }

    private void ensureBudgetIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        budgetPlanRepository.findById(id)
                .filter(budget -> !budget.getStudentId().equals(authStudentId))
                .ifPresent(budget -> {
                    throw new NotFoundException();
                });
    }
}
