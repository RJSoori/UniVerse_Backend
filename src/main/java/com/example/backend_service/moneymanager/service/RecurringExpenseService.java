package com.example.backend_service.moneymanager.service;

import com.example.backend_service.moneymanager.model.RecurringExpense;
import com.example.backend_service.moneymanager.model.TransactionRecord;
import com.example.backend_service.moneymanager.model.TransactionType;
import com.example.backend_service.moneymanager.repository.RecurringExpenseRepository;
import com.example.backend_service.moneymanager.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RecurringExpenseService {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseService.class);

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final TransactionRepository transactionRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringExpenseRepository,
                                   TransactionRepository transactionRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        List<RecurringExpense> expenses = recurringExpenseRepository.findAll();
        int processed = 0;
        for (RecurringExpense expense : expenses) {
            try {
                if (isDue(expense, today)) {
                    TransactionRecord tx = new TransactionRecord(
                            TransactionType.EXPENSE,
                            expense.getAmount(),
                            expense.getCategory(),
                            expense.getWalletId(),
                            expense.getTitle(),
                            todayStr,
                            null,
                            true,
                            expense.getId()
                    );
                    tx.setStudentId(expense.getStudentId());
                    transactionRepository.save(tx);
                    expense.setLastProcessedDate(todayStr);
                    recurringExpenseRepository.save(expense);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Failed to process recurring expense id={}: {}", expense.getId(), e.getMessage(), e);
            }
        }
        if (processed > 0) {
            log.info("Processed {} recurring expense(s) for {}", processed, todayStr);
        }
    }

    private boolean isDue(RecurringExpense expense, LocalDate today) {
        try {
            LocalDate start = LocalDate.parse(expense.getStartDate());
            if (today.isBefore(start)) return false;

            if (expense.getEndDate() != null && !expense.getEndDate().isBlank()) {
                LocalDate end = LocalDate.parse(expense.getEndDate());
                if (today.isAfter(end)) return false;
            }

            LocalDate nextDue;
            if (expense.getLastProcessedDate() == null || expense.getLastProcessedDate().isBlank()) {
                nextDue = start;
            } else {
                LocalDate lastProcessed = LocalDate.parse(expense.getLastProcessedDate());
                nextDue = computeNextDue(lastProcessed, expense.getFrequency());
            }
            return !today.isBefore(nextDue);
        } catch (DateTimeParseException e) {
            log.warn("Skipping recurring expense id={} — unparseable date: {}", expense.getId(), e.getMessage());
            return false;
        }
    }

    private LocalDate computeNextDue(LocalDate from, String frequency) {
        return switch (frequency == null ? "monthly" : frequency.toLowerCase()) {
            case "daily" -> from.plusDays(1);
            case "weekly" -> from.plusWeeks(1);
            case "biweekly" -> from.plusWeeks(2);
            case "quarterly" -> from.plusMonths(3);
            case "yearly" -> from.plusYears(1);
            default -> from.plusMonths(1);
        };
    }
}
