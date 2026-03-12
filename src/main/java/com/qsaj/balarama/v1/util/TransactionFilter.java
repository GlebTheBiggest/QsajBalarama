package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Setter
public class TransactionFilter {
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionStatus status;
    private CategoryGroup categoryGroup;
    private CategoryItem categoryItem;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String searchText;

    public List<Transaction> filter(List<Transaction> transactions) {
        return transactions.stream().filter(this::matchesFilter).collect(Collectors.toList());
    }

    private boolean matchesFilter(Transaction t) {
        if (startDate != null && t.getDate().isBefore(startDate)) return false;
        if (endDate != null && t.getDate().isAfter(endDate)) return false;
        if (status != null && t.getStatus() != status) return false;

        // Виправлено: порівнюємо по ID замість об'єктів
        if (categoryGroup != null && !t.getCategoryItem().getGroup().getId().equals(categoryGroup.getId())) return false;
        if (categoryItem != null && !t.getCategoryItem().getId().equals(categoryItem.getId())) return false;

        if (minAmount != null && t.getAmount().compareTo(minAmount) < 0) return false;
        if (maxAmount != null && t.getAmount().compareTo(maxAmount) > 0) return false;
        if (searchText != null && !searchText.isEmpty()) {
            String search = searchText.toLowerCase();
            return t.getTitle().toLowerCase().contains(search) ||
                    (t.getNotes() != null && t.getNotes().toLowerCase().contains(search));
        }
        return true;
    }
}