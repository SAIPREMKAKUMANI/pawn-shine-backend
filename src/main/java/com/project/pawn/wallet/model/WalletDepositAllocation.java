package com.project.pawn.wallet.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records which specific deposit was consumed during a wallet withdrawal.
 * Enables FIFO/LIFO traceability on redemption bills.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallet_deposit_allocations")
public class WalletDepositAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdrawal_tx_id", nullable = false)
    private Long withdrawalTxId;

    @Column(name = "deposit_tx_id", nullable = false)
    private Long depositTxId;

    @Column(name = "amount_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountUsed;

    /**
     * "PRINCIPAL" or "INTEREST" — indicates what this portion of the deposit covered.
     */
    @Column(name = "allocation_type", nullable = false, length = 20)
    private String allocationType;

    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
