package com.project.pawn.billing.model;

import com.project.pawn.billing.enums.BillType;
import com.project.pawn.wallet.model.WalletDepositAllocation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_id", nullable = false, unique = true, length = 50)
    private String billId;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", nullable = false, length = 10)
    @Builder.Default
    private BillType billType = BillType.PLEDGE;

    @Column(name = "total_amount_lended", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountLended = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "interest_accumulated", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal interestAccumulated = BigDecimal.ZERO;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillItem> billItems = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillAccount> billAccounts = new ArrayList<>();

    /**
     * Transient holder for wallet allocations during redemption processing.
     * Not persisted — allocations are saved separately via WalletDepositAllocationRepository.
     */
    @Transient
    private List<WalletDepositAllocation> walletAllocations;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.billDate == null) {
            this.billDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addBillItem(BillItem item) {
        billItems.add(item);
        item.setBill(this);
    }

    public void addBillAccount(BillAccount account) {
        billAccounts.add(account);
        account.setBill(this);
    }
}
