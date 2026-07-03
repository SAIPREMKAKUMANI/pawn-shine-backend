package com.project.pawn.pledge.model;

import com.project.pawn.billing.model.ItemImage;
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
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ornament_id", nullable = false)
    private Long ornamentId;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight_gross", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightGross;

    @Column(name = "weight_net", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightNet;

    @Column(name = "amount_lended", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountLended;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "compound_interest", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal compoundInterest = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "pledge_date", nullable = false)
    private LocalDate pledgeDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private Integer gracePeriodDays = 30;

    @Column(name = "redeemed_date")
    private LocalDate redeemedDate;

    @Column(name = "defaulted_date")
    private LocalDate defaultedDate;

    @Column(name = "auctioned_date")
    private LocalDate auctionedDate;

    @Column(name = "auction_amount", precision = 15, scale = 2)
    private BigDecimal auctionAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ItemImage> itemImages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.pledgeDate == null) {
            this.pledgeDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the current outstanding balance: principal + interest - paid.
     */
    public BigDecimal getOutstandingBalance() {
        return amountLended.add(compoundInterest).subtract(paidAmount);
    }
}
