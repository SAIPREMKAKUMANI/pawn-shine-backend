package com.project.pawn.billing.service.validations;

import com.project.pawn.accounts.model.Account;
import com.project.pawn.accounts.repository.AccountRepository;
import com.project.pawn.billing.exception.BillValidationException;
import com.project.pawn.billing.dto.request.BillAccountRequestDto;
import com.project.pawn.billing.dto.request.BillItemRequestDto;
import com.project.pawn.common.exception.ErrorDetail;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.pledge.repository.OrnamentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class BillValidationService {

    private final CustomerRepository customerRepository;
    private final OrnamentRepository ornamentRepository;
    private final AccountRepository accountRepository;

    public void validateCustomerExists(Long custId) {
        if (!customerRepository.existsById(custId)) {
            log.info("Bill validation failed: customer not found [custId={}]", custId);
            throw new BillValidationException("Customer not found with ID: " + custId,
                    List.of(new ErrorDetail("custId", "Customer not found with ID: " + custId)));
        }
    }

    public void validateItemsNotEmpty(List<BillItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            log.info("Bill validation failed: no items provided");
            throw new BillValidationException("At least one item is required",
                    List.of(new ErrorDetail("items", "At least one item is required")));
        }
    }

    public void validateAccountsNotEmpty(List<BillAccountRequestDto> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            log.info("Bill validation failed: no payment accounts provided");
            throw new BillValidationException("At least one payment account is required",
                    List.of(new ErrorDetail("accounts", "At least one payment account is required")));
        }
    }

    public void validateItems(List<BillItemRequestDto> items) {
        if (items != null) {
            for (BillItemRequestDto item : items) {
                validateOrnamentId(item.getOrnamentId());
                validateItemDescription(item.getDescription());
                validateItemWeights(item.getWeightGross(), item.getWeightNet());
                validateDueDate(item.getDueDate());
                validateItemImages(item.getItemImage());
            }
        }
    }

    public void validateItemImages(List<MultipartFile> itemImages) {
        if (itemImages == null || itemImages.isEmpty()) {
            log.info("Bill validation failed: no item images provided");
            throw new BillValidationException("At least one item image is required",
                    List.of(new ErrorDetail("itemImages", "At least one item image is required")));
        }
        if (itemImages.size() > 4) {
            log.info("Bill validation failed: too many item images [count={}]", itemImages.size());
            throw new BillValidationException("A maximum of 4 item images are allowed",
                    List.of(new ErrorDetail("itemImages", "A maximum of 4 item images are allowed, got " + itemImages.size())));
        }
        for (MultipartFile image : itemImages) {
            if (image == null || image.isEmpty()) {
                log.info("Bill validation failed: item image file is empty");
                throw new BillValidationException("Item image file must not be empty",
                        List.of(new ErrorDetail("itemImages", "Item image file must not be empty")));
            }
        }
    }

    public void validateOrnamentId(Long ornamentId) {
        if (ornamentId == null) {
            log.info("Bill validation failed: ornament ID is null");
            throw new BillValidationException("Ornament ID is required",
                    List.of(new ErrorDetail("ornamentId", "Ornament ID is required")));
        }
        if (!ornamentRepository.existsById(ornamentId)) {
            log.info("Bill validation failed: ornament not found [ornamentId={}]", ornamentId);
            throw new BillValidationException("Ornament not found with ID: " + ornamentId,
                    List.of(new ErrorDetail("ornamentId", "Ornament not found with ID: " + ornamentId)));
        }
    }

    public void validateItemDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            log.info("Bill validation failed: item description is empty");
            throw new BillValidationException("Item description is required",
                    List.of(new ErrorDetail("description", "Item description is required")));
        }
    }

    public void validateItemWeights(BigDecimal weightGross, BigDecimal weightNet) {
        if (weightGross == null) {
            log.info("Bill validation failed: gross weight is null");
            throw new BillValidationException("Gross weight is required",
                    List.of(new ErrorDetail("weightGross", "Gross weight is required")));
        }
        if (weightNet == null) {
            log.info("Bill validation failed: net weight is null");
            throw new BillValidationException("Net weight is required",
                    List.of(new ErrorDetail("weightNet", "Net weight is required")));
        }
        if (weightNet.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Bill validation failed: net weight <= 0 [weightNet={}]", weightNet);
            throw new BillValidationException("Net weight must be greater than 0",
                    List.of(new ErrorDetail("weightNet", "Net weight must be greater than 0")));
        }
        if (weightGross.compareTo(weightNet) < 0) {
            log.info("Bill validation failed: gross weight <= net weight [gross={}, net={}]", weightGross, weightNet);
            throw new BillValidationException("Gross weight must be greater than net weight",
                    List.of(new ErrorDetail("weightGross", "Gross weight must be greater than net weight")));
        }
    }

    public void validateDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            log.info("Bill validation failed: due date is null");
            throw new BillValidationException("Due date is required",
                    List.of(new ErrorDetail("dueDate", "Due date is required")));
        }
        if (dueDate.isBefore(LocalDate.now()) || dueDate.isEqual(LocalDate.now())) {
            log.info("Bill validation failed: due date is not in the future [dueDate={}]", dueDate);
            throw new BillValidationException("Due date must be in the future",
                    List.of(new ErrorDetail("dueDate", "Due date must be in the future")));
        }
    }

    public void validateAccounts(List<BillAccountRequestDto> accounts) {
        List<ErrorDetail> errorDetails = new ArrayList<>();
        if (accounts != null) {
            for (BillAccountRequestDto account : accounts) {
                if (account.getAccountId() == null) {
                    log.info("Bill validation failed: account ID is null");
                    errorDetails.add(new ErrorDetail("accountId", "Account ID should not be null or empty"));
                    throw new BillValidationException("Invalid account", errorDetails);
                }
                Optional<Account> optionalAccount = accountRepository.findById(account.getAccountId());
                if (optionalAccount.isEmpty()) {
                    log.info("Bill validation failed: account not found [accountNumber={}]", account.getAccountNumber());
                    errorDetails.add(new ErrorDetail("accountNumber", "Account not found with number: " + account.getAccountNumber()));
                    throw new BillValidationException("Invalid account", errorDetails);
                }
            }
        }
    }
}
