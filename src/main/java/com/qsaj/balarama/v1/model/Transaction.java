package com.qsaj.balarama.v1.model;

import com.qsaj.balarama.v1.model.enums.Priority;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "category_item_id", nullable = true)
    private CategoryItem categoryItem;

    @Column(name = "snapshot_group_name")
    private String snapshotGroupName;

    @Column(name = "snapshot_item_name")
    private String snapshotItemName;

    @Column(name = "snapshot_color")
    private String snapshotColor;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "budget_period_id", nullable = true)
    private BudgetPeriod budgetPeriod;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (date == null) date = LocalDate.now();
        if (status == null) status = TransactionStatus.COMPLETED;
        if (priority == null) priority = Priority.STANDARD;

        if (categoryItem != null) {
            this.snapshotItemName = categoryItem.getName();
            if (categoryItem.getGroup() != null) {
                this.snapshotGroupName = categoryItem.getGroup().getName();
                this.snapshotColor = categoryItem.getGroup().getColor();
            }
        }
    }
}