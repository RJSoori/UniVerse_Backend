package com.example.backend_service.moneymanager.web;

import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import com.example.backend_service.moneymanager.model.BudgetPlan;
import com.example.backend_service.moneymanager.model.CategoryBudget;
import com.example.backend_service.moneymanager.model.MoneyManagerSettings;
import com.example.backend_service.moneymanager.model.RecurringExpense;
import com.example.backend_service.moneymanager.model.TransactionRecord;
import com.example.backend_service.moneymanager.model.TransactionType;
import com.example.backend_service.moneymanager.model.Wallet;
import com.example.backend_service.moneymanager.repository.BudgetPlanRepository;
import com.example.backend_service.moneymanager.repository.CategoryBudgetRepository;
import com.example.backend_service.moneymanager.repository.MoneyManagerSettingsRepository;
import com.example.backend_service.moneymanager.repository.RecurringExpenseRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import com.example.backend_service.moneymanager.repository.WalletRepository;
import com.example.backend_service.moneymanager.service.BudgetValidationService;
import com.example.backend_service.notifications.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
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

@Validated
@RestController
@RequestMapping("/api/money-manager")
public class MoneyManagerController {

    private static final Logger log = LoggerFactory.getLogger(MoneyManagerController.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final MoneyManagerSettingsRepository settingsRepository;
    private final BudgetValidationService budgetValidationService;
    private final PushNotificationService pushNotificationService;

    public MoneyManagerController(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            CategoryBudgetRepository categoryBudgetRepository,
            BudgetPlanRepository budgetPlanRepository,
            MoneyManagerSettingsRepository settingsRepository,
            BudgetValidationService budgetValidationService,
            PushNotificationService pushNotificationService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.budgetPlanRepository = budgetPlanRepository;
        this.settingsRepository = settingsRepository;
        this.budgetValidationService = budgetValidationService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/wallets")
    public List<Wallet> getWallets(@AuthenticationPrincipal Long authStudentId) {
        return walletRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public Wallet createWallet(@AuthenticationPrincipal Long authStudentId, @Valid @RequestBody Wallet wallet) {
        wallet.setId(null);
        wallet.setStudentId(authStudentId);
        Wallet saved = walletRepository.save(wallet);
        log.info("student={} created wallet id={} name={}", authStudentId, saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    @PutMapping("/wallets/bulk")
    public List<Wallet> replaceWallets(@AuthenticationPrincipal Long authStudentId,
            @Size(max = 200) @Valid @RequestBody List<Wallet> wallets) {
        wallets.forEach(wallet -> ensureWalletIdIsNewOrOwned(wallet.getId(), authStudentId));
        walletRepository.deleteByStudentId(authStudentId);
        walletRepository.flush();
        wallets.forEach(wallet -> wallet.setStudentId(authStudentId));
        List<Wallet> saved = walletRepository.saveAll(wallets);
        log.info("student={} replaced wallets count={}", authStudentId, saved.size());
        return saved;
    }

    @PutMapping("/wallets/{id}")
    public Wallet updateWallet(@AuthenticationPrincipal Long authStudentId, @PathVariable String id,
            @Valid @RequestBody Wallet wallet) {
        walletRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        wallet.setId(id);
        wallet.setStudentId(authStudentId);
        return walletRepository.save(wallet);
    }

    @Transactional
    @DeleteMapping("/wallets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallet(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        walletRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        transactionRepository.deleteByWalletId(id);
        recurringExpenseRepository.deleteByWalletId(id);
        walletRepository.deleteById(id);
    }

    @GetMapping("/transactions")
    public List<TransactionRecord> getTransactions(@AuthenticationPrincipal Long authStudentId) {
        return transactionRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionRecord createTransaction(@AuthenticationPrincipal Long authStudentId,
            @Valid @RequestBody TransactionRecord transactionRecord) {
        transactionRecord.setId(null);
        ensureWalletBelongsToStudent(transactionRecord.getWalletId(), authStudentId);
        transactionRecord.setStudentId(authStudentId);
        if (TransactionType.EXPENSE.equals(transactionRecord.getType())
                && transactionRecord.getDate() != null && transactionRecord.getDate().length() >= 7) {
            String month = transactionRecord.getDate().substring(0, 7);
            if (budgetValidationService.wouldExceedBudget(authStudentId, transactionRecord.getCategory(),
                    transactionRecord.getAmount(), month)) {
                log.warn("student={} budget exceeded for category={} month={}", authStudentId,
                        transactionRecord.getCategory(), month);
                pushNotificationService.sendToStudent(authStudentId, "Budget exceeded",
                        "You've gone over your " + transactionRecord.getCategory() + " budget for " + month + ".");
            }
        }
        TransactionRecord saved = transactionRepository.save(transactionRecord);
        log.info("student={} created transaction id={} type={} amount={}", authStudentId, saved.getId(),
                saved.getType(), saved.getAmount());
        return saved;
    }

    @Transactional
    @PutMapping("/transactions/bulk")
    public List<TransactionRecord> replaceTransactions(@AuthenticationPrincipal Long authStudentId,
            @Size(max = 200) @Valid @RequestBody List<TransactionRecord> transactions) {
        transactions.forEach(transaction -> {
            ensureTransactionIdIsNewOrOwned(transaction.getId(), authStudentId);
            ensureWalletBelongsToStudent(transaction.getWalletId(), authStudentId);
        });
        transactionRepository.deleteByStudentId(authStudentId);
        transactionRepository.flush();
        transactions.forEach(transaction -> transaction.setStudentId(authStudentId));
        List<TransactionRecord> saved = transactionRepository.saveAll(transactions);
        log.info("student={} replaced transactions count={}", authStudentId, saved.size());
        return saved;
    }

    @PutMapping("/transactions/{id}")
    public TransactionRecord updateTransaction(@AuthenticationPrincipal Long authStudentId, @PathVariable String id,
            @Valid @RequestBody TransactionRecord transactionRecord) {
        transactionRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        ensureWalletBelongsToStudent(transactionRecord.getWalletId(), authStudentId);
        transactionRecord.setId(id);
        transactionRecord.setStudentId(authStudentId);
        return transactionRepository.save(transactionRecord);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        transactionRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        transactionRepository.deleteById(id);
    }

    @GetMapping("/recurring-expenses")
    public List<RecurringExpense> getRecurringExpenses(@AuthenticationPrincipal Long authStudentId) {
        return recurringExpenseRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/recurring-expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpense createRecurringExpense(@AuthenticationPrincipal Long authStudentId,
            @Valid @RequestBody RecurringExpense recurringExpense) {
        recurringExpense.setId(null);
        ensureWalletBelongsToStudent(recurringExpense.getWalletId(), authStudentId);
        recurringExpense.setStudentId(authStudentId);
        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);
        log.info("student={} created recurring expense id={} title={}", authStudentId, saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    @PutMapping("/recurring-expenses/bulk")
    public List<RecurringExpense> replaceRecurringExpenses(@AuthenticationPrincipal Long authStudentId,
            @Size(max = 200) @Valid @RequestBody List<RecurringExpense> recurringExpenses) {
        recurringExpenses.forEach(recurringExpense -> {
            ensureRecurringExpenseIdIsNewOrOwned(recurringExpense.getId(), authStudentId);
            ensureWalletBelongsToStudent(recurringExpense.getWalletId(), authStudentId);
        });
        recurringExpenseRepository.deleteByStudentId(authStudentId);
        recurringExpenseRepository.flush();
        recurringExpenses.forEach(recurringExpense -> recurringExpense.setStudentId(authStudentId));
        List<RecurringExpense> saved = recurringExpenseRepository.saveAll(recurringExpenses);
        log.info("student={} replaced recurring expenses count={}", authStudentId, saved.size());
        return saved;
    }

    @PutMapping("/recurring-expenses/{id}")
    public RecurringExpense updateRecurringExpense(@AuthenticationPrincipal Long authStudentId,
            @PathVariable String id, @Valid @RequestBody RecurringExpense recurringExpense) {
        recurringExpenseRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        ensureWalletBelongsToStudent(recurringExpense.getWalletId(), authStudentId);
        recurringExpense.setId(id);
        recurringExpense.setStudentId(authStudentId);
        return recurringExpenseRepository.save(recurringExpense);
    }

    @DeleteMapping("/recurring-expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurringExpense(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        recurringExpenseRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        recurringExpenseRepository.deleteById(id);
    }

    @GetMapping("/category-budgets")
    public List<CategoryBudget> getCategoryBudgets(@AuthenticationPrincipal Long authStudentId) {
        return categoryBudgetRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/category-budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBudget createCategoryBudget(@AuthenticationPrincipal Long authStudentId,
            @Valid @RequestBody CategoryBudget categoryBudget) {
        categoryBudget.setId(null);
        categoryBudget.setStudentId(authStudentId);
        CategoryBudget saved = categoryBudgetRepository.save(categoryBudget);
        log.info("student={} created category budget id={} category={} month={}", authStudentId,
                saved.getId(), saved.getCategory(), saved.getMonth());
        return saved;
    }

    @Transactional
    @PutMapping("/category-budgets/bulk")
    public List<CategoryBudget> replaceCategoryBudgets(@AuthenticationPrincipal Long authStudentId,
            @Size(max = 200) @Valid @RequestBody List<CategoryBudget> categoryBudgets) {
        categoryBudgets.forEach(categoryBudget -> ensureCategoryBudgetIdIsNewOrOwned(categoryBudget.getId(), authStudentId));
        categoryBudgetRepository.deleteByStudentId(authStudentId);
        categoryBudgetRepository.flush();
        categoryBudgets.forEach(categoryBudget -> categoryBudget.setStudentId(authStudentId));
        List<CategoryBudget> saved = categoryBudgetRepository.saveAll(categoryBudgets);
        log.info("student={} replaced category budgets count={}", authStudentId, saved.size());
        return saved;
    }

    @PutMapping("/category-budgets/{id}")
    public CategoryBudget updateCategoryBudget(@AuthenticationPrincipal Long authStudentId, @PathVariable String id,
            @Valid @RequestBody CategoryBudget categoryBudget) {
        categoryBudgetRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        categoryBudget.setId(id);
        categoryBudget.setStudentId(authStudentId);
        return categoryBudgetRepository.save(categoryBudget);
    }

    @DeleteMapping("/category-budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategoryBudget(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        categoryBudgetRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        categoryBudgetRepository.deleteById(id);
    }

    @GetMapping("/budgets/current")
    public List<BudgetPlan> getBudgets(@AuthenticationPrincipal Long authStudentId) {
        return budgetPlanRepository.findByStudentId(authStudentId);
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetPlan saveBudget(@AuthenticationPrincipal Long authStudentId, @Valid @RequestBody BudgetPlan budgetPlan) {
        budgetPlan.setId(null);
        budgetPlan.setStudentId(authStudentId);
        BudgetPlan saved = budgetPlanRepository.save(budgetPlan);
        log.info("student={} saved budget id={} month={}", authStudentId, saved.getId(), saved.getMonth());
        return saved;
    }

    @Transactional
    @PutMapping("/budgets/bulk")
    public List<BudgetPlan> replaceBudgets(@AuthenticationPrincipal Long authStudentId,
            @Size(max = 200) @Valid @RequestBody List<BudgetPlan> budgets) {
        budgets.forEach(budget -> ensureBudgetIdIsNewOrOwned(budget.getId(), authStudentId));
        budgetPlanRepository.deleteByStudentId(authStudentId);
        budgetPlanRepository.flush();
        budgets.forEach(budget -> budget.setStudentId(authStudentId));
        List<BudgetPlan> saved = budgetPlanRepository.saveAll(budgets);
        log.info("student={} replaced budgets count={}", authStudentId, saved.size());
        return saved;
    }

    @PutMapping("/budgets/{id}")
    public BudgetPlan updateBudget(@AuthenticationPrincipal Long authStudentId, @PathVariable String id,
            @Valid @RequestBody BudgetPlan budgetPlan) {
        budgetPlanRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        budgetPlan.setId(id);
        budgetPlan.setStudentId(authStudentId);
        return budgetPlanRepository.save(budgetPlan);
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@AuthenticationPrincipal Long authStudentId, @PathVariable String id) {
        budgetPlanRepository.findByIdAndStudentId(id, authStudentId)
                .orElseThrow(NotFoundException::new);
        budgetPlanRepository.deleteById(id);
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
            throw new NotFoundException();
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
                    throw new ForbiddenException();
                });
    }

    private void ensureTransactionIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        transactionRepository.findById(id)
                .filter(transaction -> !transaction.getStudentId().equals(authStudentId))
                .ifPresent(transaction -> {
                    throw new ForbiddenException();
                });
    }

    private void ensureRecurringExpenseIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        recurringExpenseRepository.findById(id)
                .filter(recurringExpense -> !recurringExpense.getStudentId().equals(authStudentId))
                .ifPresent(recurringExpense -> {
                    throw new ForbiddenException();
                });
    }

    private void ensureCategoryBudgetIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        categoryBudgetRepository.findById(id)
                .filter(categoryBudget -> !categoryBudget.getStudentId().equals(authStudentId))
                .ifPresent(categoryBudget -> {
                    throw new ForbiddenException();
                });
    }

    private void ensureBudgetIdIsNewOrOwned(String id, Long authStudentId) {
        if (id == null || id.isBlank()) {
            return;
        }
        budgetPlanRepository.findById(id)
                .filter(budget -> !budget.getStudentId().equals(authStudentId))
                .ifPresent(budget -> {
                    throw new ForbiddenException();
                });
    }
}
