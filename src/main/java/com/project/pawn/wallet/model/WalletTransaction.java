package com.project.pawn.wallet.model;

import com.project.pawn.wallet.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WalletTransactionType type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * For DEPOSIT transactions only: tracks how much of this deposit
     * is still available for future withdrawals. Starts at 'amount'
     * and decreases as allocations consume it. NULL for WITHDRAWAL transactions.
     */
    @Column(name = "remaining_balance", precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }
}
