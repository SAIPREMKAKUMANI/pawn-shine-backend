package com.project.pawn.pledge.service;

import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.pledge.dto.ItemDto;
import com.project.pawn.pledge.enums.ItemStatus;
import com.project.pawn.pledge.mapper.PledgeMapper;
import com.project.pawn.pledge.model.Item;
import com.project.pawn.pledge.model.Ornament;
import com.project.pawn.pledge.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final OrnamentService ornamentService;
    private final CustomerRepository customerRepository;
    private final PledgeMapper pledgeMapper;

    /**
     * Creates a new pledged item. Called by BillService during pledge creation.
     */
    @Transactional
    public Item createItem(Long custId, Long ornamentId, String description,
                           BigDecimal weightGross, BigDecimal weightNet,
                           BigDecimal amountLended, BigDecimal interestRate,
                           String location, LocalDate dueDate, Integer gracePeriodDays,
                           String imageUrl) {
        log.info("Creating pledged item for customer {} ornament {}", custId, ornamentId);

        validateCustomerExists(custId);
        ornamentService.findOrnamentOrThrow(ornamentId);

        Item item = Item.builder()
                .custId(custId)
                .ornamentId(ornamentId)
                .description(description)
                .imageUrl(imageUrl)
                .weightGross(weightGross)
                .weightNet(weightNet)
                .amountLended(amountLended)
                .interestRate(interestRate)
                .paidAmount(BigDecimal.ZERO)
                .compoundInterest(BigDecimal.ZERO)
                .status(ItemStatus.ACTIVE.name())
                .location(location)
                .pledgeDate(LocalDate.now())
                .dueDate(dueDate)
                .gracePeriodDays(gracePeriodDays != null ? gracePeriodDays : 30)
                .build();

        Item saved = itemRepository.save(item);
        log.info("Item created with ID: {}", saved.getId());
        return saved;
    }

    public ItemDto getItemById(Long id) {
        Item item = findItemOrThrow(id);
        return enrichItemDto(pledgeMapper.toItemDto(item), item);
    }

    public List<ItemDto> getItemsByCustomer(Long custId) {
        List<Item> items = itemRepository.findByCustId(custId);
        return items.stream()
                .map(item -> enrichItemDto(pledgeMapper.toItemDto(item), item))
                .toList();
    }

    public List<ItemDto> getActiveItemsByCustomer(Long custId) {
        List<Item> items = itemRepository.findByCustIdAndStatus(custId, ItemStatus.ACTIVE.name());
        return items.stream()
                .map(item -> enrichItemDto(pledgeMapper.toItemDto(item), item))
                .toList();
    }

    public Page<ItemDto> getItemsByStatus(ItemStatus status, int page, int size) {
        return itemRepository.findByStatus(status.name(), PageRequest.of(page, size))
                .map(item -> enrichItemDto(pledgeMapper.toItemDto(item), item));
    }

    /**
     * Marks item as redeemed when customer pays back.
     */
    @Transactional
    public Item redeemItem(Long itemId, BigDecimal paymentAmount) {
        Item item = findItemOrThrow(itemId);
        validateItemStatus(item, ItemStatus.ACTIVE, ItemStatus.HOLD);

        item.setPaidAmount(item.getPaidAmount().add(paymentAmount));
        item.setStatus(ItemStatus.REDEEMED.name());
        item.setRedeemedDate(LocalDate.now());

        Item saved = itemRepository.save(item);
        log.info("Item {} redeemed", itemId);
        return saved;
    }

    /**
     * Marks item as defaulted when grace period expires.
     */
    @Transactional
    public Item defaultItem(Long itemId) {
        Item item = findItemOrThrow(itemId);
        item.setStatus(ItemStatus.DEFAULTED.name());
        item.setDefaultedDate(LocalDate.now());

        Item saved = itemRepository.save(item);
        log.info("Item {} defaulted", itemId);
        return saved;
    }

    /**
     * Marks item as on hold (grace period started).
     */
    @Transactional
    public Item holdItem(Long itemId) {
        Item item = findItemOrThrow(itemId);
        item.setStatus(ItemStatus.HOLD.name());

        Item saved = itemRepository.save(item);
        log.info("Item {} placed on hold", itemId);
        return saved;
    }

    /**
     * Marks item as auctioned with the sale amount.
     */
    @Transactional
    public Item auctionItem(Long itemId, BigDecimal auctionAmount) {
        Item item = findItemOrThrow(itemId);
        validateItemStatus(item, ItemStatus.DEFAULTED);

        item.setStatus(ItemStatus.AUCTIONED.name());
        item.setAuctionedDate(LocalDate.now());
        item.setAuctionAmount(auctionAmount);

        Item saved = itemRepository.save(item);
        log.info("Item {} auctioned for {}", itemId, auctionAmount);
        return saved;
    }

    /**
     * Updates compound interest on an item. Called by InterestService.
     */
    @Transactional
    public void updateInterest(Long itemId, BigDecimal newCompoundInterest) {
        Item item = findItemOrThrow(itemId);
        item.setCompoundInterest(newCompoundInterest);
        itemRepository.save(item);
    }

    /**
     * Records a partial payment on an active item.
     */
    @Transactional
    public Item recordPayment(Long itemId, BigDecimal paymentAmount) {
        Item item = findItemOrThrow(itemId);
        validateItemStatus(item, ItemStatus.ACTIVE, ItemStatus.HOLD);

        item.setPaidAmount(item.getPaidAmount().add(paymentAmount));
        Item saved = itemRepository.save(item);
        log.info("Payment of {} recorded on item {}", paymentAmount, itemId);
        return saved;
    }

    // --- Dashboard queries ---

    public Map<String, Object> getDashboardStats() {
        BigDecimal totalOutstanding = itemRepository.totalOutstandingBalance();
        long activeCount = itemRepository.countByStatus(ItemStatus.ACTIVE.name());
        long defaultedCount = itemRepository.countByStatus(ItemStatus.DEFAULTED.name());
        long holdCount = itemRepository.countByStatus(ItemStatus.HOLD.name());
        BigDecimal totalLended = itemRepository.sumAmountLendedByStatus(ItemStatus.ACTIVE.name());

        return Map.of(
                "total_outstanding", totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO,
                "active_count", activeCount,
                "defaulted_count", defaultedCount,
                "hold_count", holdCount,
                "total_lended_active", totalLended != null ? totalLended : BigDecimal.ZERO
        );
    }

    /**
     * Finds items past due date that need status transition.
     * Called by a scheduled job.
     */
    public List<Item> findOverdueActiveItems() {
        return itemRepository.findByStatusAndDueDateBefore(ItemStatus.ACTIVE.name(), LocalDate.now());
    }

    public List<Item> findExpiredHoldItems() {
        List<Item> holdItems = itemRepository.findByStatusAndDueDateBeforeAndGracePeriodDaysIsNotNull(
                ItemStatus.HOLD.name(), LocalDate.now());
        return holdItems.stream()
                .filter(item -> {
                    LocalDate graceEnd = item.getDueDate().plusDays(item.getGracePeriodDays());
                    return LocalDate.now().isAfter(graceEnd);
                })
                .toList();
    }

    // --- Private helpers ---

    public Item findItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with ID: " + id));
    }

    private void validateCustomerExists(Long custId) {
        if (!customerRepository.existsById(custId)) {
            throw new IllegalArgumentException("Customer not found with ID: " + custId);
        }
    }

    private void validateItemStatus(Item item, ItemStatus... allowedStatuses) {
        for (ItemStatus allowed : allowedStatuses) {
            if (allowed.name().equals(item.getStatus())) {
                return;
            }
        }
        throw new IllegalStateException(
                "Item " + item.getId() + " has status " + item.getStatus() +
                        ", expected one of: " + java.util.Arrays.toString(allowedStatuses));
    }

    private ItemDto enrichItemDto(ItemDto dto, Item item) {
        ornamentService.findOrnamentOrThrow(item.getOrnamentId());
        Ornament ornament = ornamentService.findOrnamentOrThrow(item.getOrnamentId());
        dto.setOrnamentType(ornament.getType());

        customerRepository.findById(item.getCustId())
                .map(CustomerInfo::getName)
                .ifPresent(dto::setCustomerName);

        return dto;
    }
}
