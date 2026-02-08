package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.Category;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionFilter {
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionStatus status;
    private Category category;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String searchText;

    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public void setCategory(Category category) { this.category = category; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public void setSearchText(String searchText) { this.searchText = searchText; }

    public List<Transaction> filter(List<Transaction> transactions) {
        return transactions.stream().filter(this::matchesFilter).collect(Collectors.toList());
    }

    private boolean matchesFilter(Transaction t) {
        if (startDate != null && t.getDate().isBefore(startDate)) return false;
        if (endDate != null && t.getDate().isAfter(endDate)) return false;
        if (status != null && t.getStatus() != status) return false;
        if (category != null && t.getCategory() != category) return false;
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