package com.example.backend_service.moneymanager.web;

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
    public List<Wallet> getWallets() {
        return walletRepository.findAll();
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public Wallet createWallet(@RequestBody Wallet wallet) {
        return walletRepository.save(wallet);
    }

    @PutMapping("/wallets/bulk")
    public List<Wallet> replaceWallets(@RequestBody List<Wallet> wallets) {
        walletRepository.deleteAllInBatch();
        return walletRepository.saveAll(wallets);
    }

    @PutMapping("/wallets/{id}")
    public Wallet updateWallet(@PathVariable String id, @RequestBody Wallet wallet) {
        wallet.setId(id);
        return walletRepository.save(wallet);
    }

    @DeleteMapping("/wallets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallet(@PathVariable String id) {
        walletRepository.deleteById(id);
    }

    @GetMapping("/transactions")
    public List<TransactionRecord> getTransactions() {
        return transactionRepository.findAll();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionRecord createTransaction(@RequestBody TransactionRecord transactionRecord) {
        return transactionRepository.save(transactionRecord);
    }

    @PutMapping("/transactions/bulk")
    public List<TransactionRecord> replaceTransactions(@RequestBody List<TransactionRecord> transactions) {
        transactionRepository.deleteAllInBatch();
        return transactionRepository.saveAll(transactions);
    }

    @GetMapping("/recurring-expenses")
    public List<RecurringExpense> getRecurringExpenses() {
        return recurringExpenseRepository.findAll();
    }

    @PostMapping("/recurring-expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpense createRecurringExpense(@RequestBody RecurringExpense recurringExpense) {
        return recurringExpenseRepository.save(recurringExpense);
    }

    @PutMapping("/recurring-expenses/bulk")
    public List<RecurringExpense> replaceRecurringExpenses(@RequestBody List<RecurringExpense> recurringExpenses) {
        recurringExpenseRepository.deleteAllInBatch();
        return recurringExpenseRepository.saveAll(recurringExpenses);
    }

    @GetMapping("/category-budgets")
    public List<CategoryBudget> getCategoryBudgets() {
        return categoryBudgetRepository.findAll();
    }

    @PostMapping("/category-budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBudget createCategoryBudget(@RequestBody CategoryBudget categoryBudget) {
        return categoryBudgetRepository.save(categoryBudget);
    }

    @PutMapping("/category-budgets/bulk")
    public List<CategoryBudget> replaceCategoryBudgets(@RequestBody List<CategoryBudget> categoryBudgets) {
        categoryBudgetRepository.deleteAllInBatch();
        return categoryBudgetRepository.saveAll(categoryBudgets);
    }

    @GetMapping("/budgets/current")
    public List<BudgetPlan> getBudgets() {
        return budgetPlanRepository.findAll();
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetPlan saveBudget(@RequestBody BudgetPlan budgetPlan) {
        return budgetPlanRepository.save(budgetPlan);
    }

    @PutMapping("/budgets/bulk")
    public List<BudgetPlan> replaceBudgets(@RequestBody List<BudgetPlan> budgets) {
        budgetPlanRepository.deleteAllInBatch();
        return budgetPlanRepository.saveAll(budgets);
    }

    @GetMapping("/settings")
    public MoneyManagerSettings getSettings() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(new MoneyManagerSettings(false, "LKR", null)));
    }

    @PutMapping("/settings")
    public MoneyManagerSettings saveSettings(@RequestBody MoneyManagerSettings settings) {
        settings.setId("default");
        return settingsRepository.save(settings);
    }
}
