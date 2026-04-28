package com.project.pawn.billing.service;

import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.service.TransactionService;
import com.project.pawn.billing.dto.*;
import com.project.pawn.billing.enums.BillItemAction;
import com.project.pawn.billing.enums.BillType;
import com.project.pawn.billing.enums.PaymentDirection;
import com.project.pawn.billing.mapper.BillingMapper;
import com.project.pawn.billing.model.Bill;
import com.project.pawn.billing.model.BillAccount;
import com.project.pawn.billing.model.BillItem;
import com.project.pawn.billing.repository.BillRepository;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.pledge.model.Item;
import com.project.pawn.pledge.service.ItemService;
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
import java.util.concurrent.atomic.AtomicLong;

import static com.project.pawn.customeronboarding.constants.Constant.USERNAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private static final DateTimeFormatter BILL_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong BILL_COUNTER = new AtomicLong(0);

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final TransactionService transactionService;
    private final BillingMapper billingMapper;

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
    public BillDto createPledgeBill(CreatePledgeBillRequest request) {
        log.info("Creating pledge bill for customer {}", request.getCustId());

        validateCustomerExists(request.getCustId());
        validateItemsNotEmpty(request.getItems());
        validateAccountsNotEmpty(request.getAccounts());

        Bill bill = Bill.builder()
                .billId(generateBillId())
                .custId(request.getCustId())
                .billType(BillType.CREDIT.name())
                .billDate(request.getBillDate() != null ? request.getBillDate() : LocalDate.now())
                .notes(request.getNotes())
                .createdBy(MDC.get(USERNAME))
                .build();

        BigDecimal totalLended = BigDecimal.ZERO;

        // Create each pledged item and add to bill
        for (BillItemDto itemReq : request.getItems()) {
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
                    itemReq.getGracePeriodDays(),
                    itemReq.getImageUrl()
            );

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
        for (BillAccountDto acctReq : request.getAccounts()) {
            BillAccount billAccount = BillAccount.builder()
                    .accountId(acctReq.getAccountId())
                    .amount(acctReq.getAmount())
                    .direction(PaymentDirection.OUT.name())
                    .build();
            bill.addBillAccount(billAccount);
        }

        Bill savedBill = billRepository.save(bill);

        // Record transactions for each account (money leaving)
        for (BillAccountDto acctReq : request.getAccounts()) {
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
    public BillDto createRedemptionBill(CreateRedemptionBillRequest request) {
        log.info("Creating redemption bill for customer {}", request.getCustId());

        validateCustomerExists(request.getCustId());
        validateAccountsNotEmpty(request.getAccounts());

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new IllegalArgumentException("At least one item ID is required for redemption");
        }

        Bill bill = Bill.builder()
                .billId(generateBillId())
                .custId(request.getCustId())
                .billType(BillType.DEBIT.name())
                .billDate(request.getBillDate() != null ? request.getBillDate() : LocalDate.now())
                .notes(request.getNotes())
                .createdBy(MDC.get(USERNAME))
                .build();

        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalLended = BigDecimal.ZERO;
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

            totalInterest = totalInterest.add(item.getCompoundInterest());
            totalLended = totalLended.add(item.getAmountLended());
            totalPayment = totalPayment.add(outstanding);
        }

        bill.setTotalAmountLended(totalLended);
        bill.setAmountPaid(totalPayment);
        bill.setInterestAccumulated(totalInterest);

        // Add payment accounts (money coming IN)
        for (BillAccountDto acctReq : request.getAccounts()) {
            BillAccount billAccount = BillAccount.builder()
                    .accountId(acctReq.getAccountId())
                    .amount(acctReq.getAmount())
                    .direction(PaymentDirection.IN.name())
                    .build();
            bill.addBillAccount(billAccount);
        }

        Bill savedBill = billRepository.save(bill);

        // Record transactions for each account (money coming in)
        for (BillAccountDto acctReq : request.getAccounts()) {
            transactionService.recordTransaction(
                    acctReq.getAccountId(),
                    acctReq.getAmount(),
                    TransactionType.CREDIT,
                    savedBill.getId(),
                    "Redemption bill " + savedBill.getBillId() + " - payment received",
                    savedBill.getBillId()
            );
        }

        log.info("Redemption bill {} created. Total paid: {}, interest: {}",
                savedBill.getBillId(), totalPayment, totalInterest);
        return enrichBillDto(billingMapper.toBillDto(savedBill));
    }

    // --- Read operations ---

    public BillDto getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + id));
        return enrichBillDto(billingMapper.toBillDto(bill));
    }

    public BillDto getBillByBillId(String billId) {
        Bill bill = billRepository.findByBillId(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        return enrichBillDto(billingMapper.toBillDto(bill));
    }

    public Page<BillDto> getBillsByCustomer(Long custId, int page, int size) {
        return billRepository.findByCustIdOrderByBillDateDesc(custId, PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    public Page<BillDto> getBillsByType(BillType type, int page, int size) {
        return billRepository.findByBillTypeOrderByBillDateDesc(type.name(), PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    public Page<BillDto> getAllBills(int page, int size) {
        return billRepository.findAllByOrderByBillDateDesc(PageRequest.of(page, size))
                .map(bill -> enrichBillDto(billingMapper.toBillDto(bill)));
    }

    // --- Private helpers ---

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

    private BillDto enrichBillDto(BillDto dto) {
        customerRepository.findById(dto.getCustId())
                .map(CustomerInfo::getName)
                .ifPresent(dto::setCustomerName);
        return dto;
    }

    private void validateCustomerExists(Long custId) {
        if (!customerRepository.existsById(custId)) {
            throw new IllegalArgumentException("Customer not found with ID: " + custId);
        }
    }

    private void validateItemsNotEmpty(List<BillItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
    }

    private void validateAccountsNotEmpty(List<BillAccountDto> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException("At least one payment account is required");
        }
    }
}
