package com.project.tracker.internal_expsense_tracker_backend.service;


import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Expense;
import com.project.tracker.internal_expsense_tracker_backend.domain.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskAnalysisEngine {
    private final ExpenseRepo expenseRepo;

    public void analyzeExpense(Expense expense) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LocalDateTime created = expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now();

        // 1. Duplicate Submissions Check (same employee, same category, similar amount ±5% within 48h)
        LocalDateTime window48h = created.minusHours(48);
        List<Expense> recentUserExpenses = expenseRepo.findByAuthorIdAndCategoryAndCreatedAtBetween(
                expense.getAuthor().getId(), expense.getCategory(), window48h, created.plusMinutes(1));

        for (Expense prev : recentUserExpenses) {
            if (!prev.getId().equals(expense.getId())) {
                BigDecimal diff = prev.getAmount().subtract(expense.getAmount()).abs();
                BigDecimal ratio = diff.divide(expense.getAmount(), 4, RoundingMode.HALF_UP);
                if (ratio.compareTo(new BigDecimal("0.05")) <= 0 || prev.getTitle().equalsIgnoreCase(expense.getTitle())) {
                    score += 35;
                    reasons.add(String.format("Potential duplicate submission detected (matches Expense #%d: '%s' of %s %s created on %s)",
                            prev.getId(), prev.getTitle(), prev.getCurrency(), prev.getAmount(), prev.getCreatedAt()));
                    break;
                }
            }
        }

        // 2. Structuring Detection (multiple expenses under $500 in 7 days summing to > $1500)
        if (expense.getAmount().compareTo(new BigDecimal("500.00")) <= 0) {
            LocalDateTime window7d = created.minusDays(7);
            List<Expense> recent7d = expenseRepo.findByAuthorIdAndCategoryAndCreatedAtBetween(
                    expense.getAuthor().getId(), expense.getCategory(), window7d, created.plusMinutes(1));

            BigDecimal sum = recent7d.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (sum.compareTo(new BigDecimal("1500.00")) > 0 && recent7d.size() >= 3) {
                score += 30;
                reasons.add(String.format("Structuring pattern detected: %d sub-$500 expenses in category '%s' within 7 days total %s %s",
                        recent7d.size(), expense.getCategory().name().toLowerCase(), expense.getCurrency(), sum));
            }
        }

        // 3. Statistical Outlier Detection (amount > 2.5x historical average for author/category)
        Double avgDouble = expenseRepo.findAverageAmountByAuthorAndCategory(
                expense.getAuthor().getId(), expense.getCategory());
        if (avgDouble == null || avgDouble == 0.0) {
            avgDouble = expenseRepo.findAverageAmountByCategory(expense.getCategory());
        }

        if (avgDouble != null && avgDouble > 0.0) {
            BigDecimal avg = BigDecimal.valueOf(avgDouble);
            BigDecimal threshold = avg.multiply(new BigDecimal("2.5"));
            if (expense.getAmount().compareTo(threshold) > 0) {
                score += 25;
                reasons.add(String.format("Statistical outlier: Amount %s %s is over 2.5x higher than historical average of %s %.2f for category '%s'",
                        expense.getCurrency(), expense.getAmount(), expense.getCurrency(), avgDouble, expense.getCategory().name().toLowerCase()));
            }
        }

        // 4. Suspicious Timing (late night 10 PM - 5 AM or weekend)
        int hour = created.getHour();
        DayOfWeek day = created.getDayOfWeek();
        if (hour >= 22 || hour < 5) {
            score += 15;
            reasons.add(String.format("Suspicious submission timing: Submitted during late night hours (%02d:%02d)", hour, created.getMinute()));
        }
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            score += 15;
            reasons.add(String.format("Suspicious submission timing: Submitted on a weekend (%s)", day.name()));
        }

        // 5. Round-Number Bias (large exact round numbers e.g. $500, $1000, $2500 above $200 threshold)
        if (expense.getAmount().compareTo(new BigDecimal("200.00")) > 0) {
            BigDecimal remainder100 = expense.getAmount().remainder(new BigDecimal("100.00"));
            if (remainder100.compareTo(BigDecimal.ZERO) == 0) {
                score += 10;
                reasons.add(String.format("Round-number bias: Exact round amount %s %s for a high-value expense",
                        expense.getCurrency(), expense.getAmount()));
            }
        }

        // Cap risk score at 100
        int finalScore = Math.min(100, score);
        RiskLevel level;
        if (finalScore >= 75) {
            level = RiskLevel.CRITICAL;
        } else if (finalScore >= 50) {
            level = RiskLevel.HIGH;
        } else if (finalScore >= 25) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        expense.setRiskScore(finalScore);
        expense.setRiskLevel(level);
        expense.setRiskReasons(reasons);
        expense.setAnalyzedAt(LocalDateTime.now());
    }
}
