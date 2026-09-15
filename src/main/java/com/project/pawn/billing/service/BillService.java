package com.project.pawn.billing.service;

import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.service.TransactionService;
import com.project.pawn.billing.dto.request.BillAccountRequestDto;
import com.project.pawn.billing.dto.request.BillItemRequestDto;
import com.project.pawn.billing.dto.request.CreatePledgeBillRequest;
import com.project.pawn.billing.dto.request.CreateRedemptionBillRequest;
import com.project.pawn.billing.dto.response.BillResponseDto;
import com.project.pawn.billing.enums.BillItemAction;
import com.project.pawn.billing.enums.BillType;
import com.project.pawn.billing.enums.PaymentDirection;
import com.project.pawn.billing.mapper.BillingMapper;
import com.project.pawn.billing.model.Bill;
import com.project.pawn.billing.model.BillAccount;
import com.project.pawn.billing.model.BillItem;
import com.project.pawn.billing.repository.BillItemRepository;
import com.project.pawn.billing.repository.BillRepository;
import com.project.pawn.billing.service.validations.BillValidationService;
import com.project.pawn.common.enums.MediaType;
import com.project.pawn.common.util.SecuritySanitizer;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.pledge.model.Item;
import com.project.pawn.pledge.service.ItemService;
import com.project.pawn.wallet.dto.WalletAllocationDto;
import com.project.pawn.wallet.model.WalletDepositAllocation;
import com.project.pawn.wallet.repository.WalletDepositAllocationRepository;
import com.project.pawn.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.project.pawn.customeronboarding.constants.Constant.USERNAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private static final DateTimeFormatter BILL_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong BILL_COUNTER = new AtomicLong(0);

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final TransactionService transactionService;
    private final BillingMapper billingMapper;
    private final WalletService walletService;
    private final WalletDepositAllocationRepository allocationRepository;
    private final BillValidationService billValidationService;
    private final ImageHandler imageHandler;
    /**
     * Creates a PLEDGE bill — lending money, keeping items as collateral.
     *
     * Flow:
     * 1. Validate customer and accounts
     * 2. Create items (pledged collateral)
     * 3. Create bill with bill_items and bill_accounts
     * 4. Record transactions (money OUT from owner accounts)
     */
    @Transactional
    public BillResponseDto createPledgeBill(CreatePledgeBillRequest request) {
        log.info("Creating pledge bill for customer {}", request.getCustId());
        sanitizePledgeRequest(request);
        billValidationService.validateCustomerExists(request.getCustId());
        billValidationService.validateItemsNotEmpty(request.getItems());
        billValidationService.validateItems(request.getItems());
        billValidationService.validateAccountsNotEmpty(request.getAccounts());
        billValidationService.validateAccounts(request.getAccounts());

        Bill bill = Bill.builder()
                .billId(generateBillId())
                .custId(request.getCustId())
                .billDate(request.getBillDate() != null ? request.getBillDate() : LocalDate.now())
                .notes(request.getNotes())
                .createdBy(MDC.get(USERNAME))
                .build();
        BigDecimal totalLended = BigDecimal.ZERO;

        // Create each pledged item and add to bill
        for (BillItemRequestDto itemReq : request.getItems()) {
            Item createdItem = itemService.createItem(
                    request.getCustId(),
                    itemReq.getOrnamentId(),
                    itemReq.getDescription(),
                    itemReq.getWeightGross(),
                    itemReq.getWeightNet(),
                    itemReq.getAmount(),
                    itemReq.getInterestRate(),
                    itemReq.getLocation(),
                    itemReq.getDueDate(),
                    itemReq.getGracePeriodDays()
            );
            imageHandler.uploadItemImages(createdItem, bill, itemReq.getItemImage(), MediaType.PLEDGE_ITEM_IMAGE);

            BillItem billItem = BillItem.builder()
                    .itemId(createdItem.getId())
                    .action(BillItemAction.KEPT.name())
                    .amount(itemReq.getAmount())
                    .build();
            bill.addBillItem(billItem);

            totalLended = totalLended.add(itemReq.getAmount());
        }

        bill.setTotalAmountLended(totalLended);

        // Add payment accounts (money going OUT)
        for (BillAccountRequestDto acctReq : request.getAccounts()) {
            BillAccount billAccount = BillAccount.builder()
                    .accountId(acctReq.getAccountId())
                    .amount(acctReq.getAmount())
                    .direction(PaymentDirection.OUT.name())
                    .build();
            bill.addBillAccount(billAccount);
        }

        Bill savedBill = billRepository.save(bill);

        // Record transactions for each account (money leaving)
        for (BillAccountRequestDto acctReq : request.getAccounts()) {
            transactionService.recordTransaction(
                    acctReq.getAccountId(),
                    acctReq.getAmount(),
                    TransactionType.DEBIT,
                    savedBill.getId(),
                    "Pledge bill " + savedBill.getBillId() + " - money lent",
                    savedBill.getBillId()
            );
        }

        log.info("Pledge bill {} created. Total lended: {}", savedBill.getBillId(), totalLended);
        return enrichBillDto(billingMapper.toBillDto(savedBill));
    }

    /**
     * Creates a REDEMPTION bill — customer paying back, releasing items.
     *
     * Flow:
     * 1. Calculate current interest on each item
     * 2. Redeem items (mark as REDEEMED)
     * 3. Create bill with bill_items and bill_accounts
     * 4. Record transactions (money IN to owner accounts)
     */
    @Transactional
    public BillResponseDto createRedemptionBill(CreateRedemptionBillRequest request) {
        sanitizeRedemptionRequest(request);

        log.info("Creating redemption bill for customer {}", request.getCustId());
        billValidationService.validateCustomerExists(request.getCustId());
        
        boolean hasAccounts = request.getAccounts() != null && !request.getAccounts().isEmpty();
        boolean usesWallet = request.getWalletAmountUsed() != null && request.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0;
        
        if (!hasAccounts && !usesWallet) {
            throw new IllegalArgumentException("At least one payment account or wallet usage is required");
        }

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new IllegalArgumentException("At least one item ID is required for redemption");
        }

        Long firstItemId = request.getItemIds().get(0);
        Bill bill = billItemRepository.findByItemId(firstItemId).stream()
                .map(BillItem::getBill)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pledge bill not found for item: " + firstItemId));

        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPayment = BigDecimal.ZERO;

        for (Long itemId : request.getItemIds()) {
            // Interest is already set by the owner — just read the current state
            Item item = itemService.findItemOrThrow(itemId);
            BigDecimal outstanding = item.getOutstandingBalance();

            // Redeem the item
            itemService.redeemItem(itemId, outstanding);

            BillItem billItem = BillItem.builder()
                    .itemId(itemId)
                    .action(BillItemAction.RELEASED.name())
                    .amount(outstanding)
                    .build();
            bill.addBillItem(billItem);

            totalPrincipal = totalPrincipal.add(item.getAmountLended());
            totalInterest = totalInterest.add(item.getCompoundInterest());
            totalPayment = totalPayment.add(outstanding);

            // Upload customer pickup photos for each redeemed item
            if (request.getItemImages() != null && !request.getItemImages().isEmpty()) {
                billValidationService.validateItemImages(request.getItemImages());
                imageHandler.uploadItemImages(item, bill, request.getItemImages(), MediaType.REDEEM_ITEM_IMAGE);
            }
        }

        BigDecimal sumAccounts = hasAccounts ? request.getAccounts().stream().map(BillAccountRequestDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        BigDecimal walletAmount = usesWallet ? request.getWalletAmountUsed() : BigDecimal.ZERO;

        if (sumAccounts.add(walletAmount).compareTo(totalPayment) != 0) {
            throw new IllegalArgumentException("Sum of account amounts and wallet usage must equal total redemption payment of " + totalPayment);
        }

        // Wallet withdrawal with FIFO/LIFO allocation
        if (usesWallet) {
            // Compute how much wallet goes to principal vs interest
            BigDecimal walletForPrincipal = walletAmount.min(totalPrincipal);
            BigDecimal walletForInterest = walletAmount.subtract(walletForPrincipal).min(totalInterest);

            List<WalletDepositAllocation> allocations = walletService.withdraw(
                    request.getCustId(), walletForPrincipal, walletForInterest,
                    bill.getBillId(), "Used for redemption of items");

            // Link allocations to the bill (bill ID set after save below)
            // Store temporarily — will set billId after bill is saved
            bill.setWalletAllocations(allocations);
        }

        bill.setAmountPaid(bill.getAmountPaid().add(totalPayment));
        bill.setInterestAccumulated(bill.getInterestAccumulated().add(totalInterest));

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            String currentNotes = bill.getNotes() != null ? bill.getNotes() : "";
            bill.setNotes(currentNotes + "\nRedemption Notes: " + request.getNotes());
        }

        // Add payment accounts (money coming IN)
        if (hasAccounts) {
            for (BillAccountRequestDto acctReq : request.getAccounts()) {
                BillAccount billAccount = BillAccount.builder()
                        .accountId(acctReq.getAccountId())
                        .amount(acctReq.getAmount())
                        .direction(PaymentDirection.IN.name())
                        .build();
                bill.addBillAccount(billAccount);
            }
        }

        Bill savedBill = billRepository.save(bill);

        // Set billId on wallet allocations now that the bill is saved
        if (usesWallet && bill.getWalletAllocations() != null) {
            for (WalletDepositAllocation alloc : bill.getWalletAllocations()) {
                alloc.setBillId(savedBill.getId());
            }
            allocationRepository.saveAll(bill.getWalletAllocations());
        }

        // Record transactions for each account (money coming in)
        if (hasAccounts) {
            for (BillAccountRequestDto acctReq : request.getAccounts()) {
                transactionService.recordTransaction(
                        acctReq.getAccountId(),
                        acctReq.getAmount(),
                        TransactionType.CREDIT,
                        savedBill.getId(),
                        "Redemption payment for bill " + savedBill.getBillId() + (request.getNotes() != null ? " - " + request.getNotes() : ""),
                        savedBill.getBillId()
                );
            }
        }

        log.info("Redemption recorded for bill {}. Total paid: {}, principal: {}, interest: {}",
                savedBill.getBillId(), totalPayment, totalPrincipal, totalInterest);

        // Check if all unique items in the bill are now RELEASED — if so, mark as REDEEM
        checkAndUpdateBillType(savedBill);

        return enrichBillDto(billingMapper.toBillDto(savedBill));
    }

    // --- Read operations ---

    public BillResponseDto getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + id));
        return enrichBillDto(billingMapper.toBillDto(bill));
    }

    public BillResponseDto getBillByBillId(String billId) {
        Bill bill = billRepository.findByBillId(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        return enrichBillDto(billingMapper.toBillDto(bill));
    }

    public Page<BillResponseDto> getBillsByCustomer(Long custId, int page, int size) {
        return billRepository.findByCustIdOrderByBillDateDesc(custId, PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    public Page<BillResponseDto> getAllBills(int page, int size) {
        return billRepository.findAllByOrderByBillDateDesc(PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    public Page<BillResponseDto> getBillsByType(BillType billType, int page, int size) {
        return billRepository.findByBillTypeOrderByBillDateDesc(billType, PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    // --- Private helpers ---

    /**
     * After a redemption, checks if ALL unique items in the bill have been released.
     * If so, transitions the bill from PLEDGE to REDEEM.
     */
    private void checkAndUpdateBillType(Bill bill) {
        Set<Long> allItemIds = bill.getBillItems().stream()
                .map(BillItem::getItemId)
                .collect(Collectors.toSet());

        Set<Long> releasedItemIds = bill.getBillItems().stream()
                .filter(bi -> BillItemAction.RELEASED.name().equals(bi.getAction()))
                .map(BillItem::getItemId)
                .collect(Collectors.toSet());

        if (!allItemIds.isEmpty() && allItemIds.equals(releasedItemIds)) {
            bill.setBillType(BillType.REDEEM);
            billRepository.save(bill);
            log.info("Bill {} fully redeemed — type set to REDEEM", bill.getBillId());
        }
    }


    private String generateBillId() {
        String datePart = LocalDate.now().format(BILL_ID_DATE_FORMAT);
        long counter = BILL_COUNTER.incrementAndGet();
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String billId = "BILL-" + datePart + "-" + timePart + "-" + String.format("%04d", counter % 10000);

        if (billRepository.existsByBillId(billId)) {
            return generateBillId();
        }
        return billId;
    }

    private BillResponseDto enrichBillDto(BillResponseDto dto) {
        customerRepository.findById(dto.getCustId())
                .map(CustomerInfo::getName)
                .ifPresent(dto::setCustomerName);

        // Enrich with wallet allocation details for redemption bills
        List<WalletAllocationDto> allocations =
                walletService.getAllocationsForBill(dto.getId());
        if (allocations != null && !allocations.isEmpty()) {
            dto.setWalletAllocations(allocations);
            dto.setWalletAmountUsed(allocations.stream()
                    .map(WalletAllocationDto::getAmountUsed)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        return dto;
    }

    // =============================================
    // SANITIZATION HELPERS
    // =============================================

    private void sanitizePledgeRequest(CreatePledgeBillRequest request) {
        request.setNotes(SecuritySanitizer.sanitizeInput(request.getNotes()));
        if (request.getItems() != null) {
            for (BillItemRequestDto item : request.getItems()) {
                item.setAction(SecuritySanitizer.sanitizeInput(item.getAction()));
                item.setDescription(SecuritySanitizer.sanitizeInput(item.getDescription()));
                item.setLocation(SecuritySanitizer.sanitizeInput(item.getLocation()));
            }
        }
        sanitizeBillAccounts(request.getAccounts());
    }

    private void sanitizeRedemptionRequest(CreateRedemptionBillRequest request) {
        request.setNotes(SecuritySanitizer.sanitizeInput(request.getNotes()));
        sanitizeBillAccounts(request.getAccounts());
    }

    private void sanitizeBillAccounts(List<BillAccountRequestDto> accounts) {
        if (accounts != null) {
            for (BillAccountRequestDto acc : accounts) {
                acc.setAccountNumber(SecuritySanitizer.sanitizeInput(acc.getAccountNumber()));
                acc.setDirection(SecuritySanitizer.sanitizeInput(acc.getDirection()));
            }
        }
    }
}
