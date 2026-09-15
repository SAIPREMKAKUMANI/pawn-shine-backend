package com.project.pawn.common.service;

import com.project.pawn.accounts.dto.AccountDto;
import com.project.pawn.authentication.payload.AuthRequest;
import com.project.pawn.authentication.payload.UsernameRequest;
import com.project.pawn.authentication.payload.WebAuthnVerifyRequest;
import com.project.pawn.billing.dto.request.BillAccountRequestDto;
import com.project.pawn.billing.dto.request.BillItemRequestDto;
import com.project.pawn.billing.dto.request.CreatePledgeBillRequest;
import com.project.pawn.billing.dto.request.CreateRedemptionBillRequest;
import com.project.pawn.billing.dto.response.BillAccountResponseDto;
import com.project.pawn.common.util.SecuritySanitizer;
import com.project.pawn.customeronboarding.dto.*;
import com.project.pawn.pledge.dto.OrnamentDto;
import com.project.pawn.wallet.dto.WalletDepositRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralized service for sanitizing all inbound request DTOs.
 * <p>
 * Each {@code sanitize()} overload strips control characters and HTML/JS-escapes
 * every user-supplied String field in the given DTO before it reaches the service layer.
 * <p>
 * <b>Design decisions:</b>
 * <ul>
 *   <li>{@link AuthRequest#getPassword()} is intentionally <b>not</b> sanitized —
 *       HTML-escaping would corrupt passwords containing {@code <>&"'} characters.</li>
 *   <li>{@link WebAuthnVerifyRequest#getResponse()} map values are <b>not</b> sanitized —
 *       they contain Base64-encoded cryptographic data from the browser WebAuthn API.</li>
 * </ul>
 */
@Service
public class RequestSanitizationService {

    // =============================================
    // Accounts
    // =============================================

    public void sanitize(AccountDto dto) {
        dto.setAccountNumber(SecuritySanitizer.sanitizeInput(dto.getAccountNumber()));
        dto.setBankName(SecuritySanitizer.sanitizeInput(dto.getBankName()));
    }

    // =============================================
    // Authentication
    // =============================================

    /**
     * Sanitizes only the username. Password is intentionally left raw
     * because it is hashed by Spring Security's AuthenticationManager.
     */
    public void sanitize(AuthRequest request) {
        request.setUsername(SecuritySanitizer.sanitizeInput(request.getUsername()));
    }

    public void sanitize(UsernameRequest request) {
        if (request != null) {
            request.setUsername(SecuritySanitizer.sanitizeInput(request.getUsername()));
        }
    }

    /**
     * Sanitizes string identity fields only. The {@code response} map contains
     * Base64-encoded cryptographic payloads and must NOT be sanitized.
     */
    public void sanitize(WebAuthnVerifyRequest request) {
        request.setUsername(SecuritySanitizer.sanitizeInput(request.getUsername()));
        request.setId(SecuritySanitizer.sanitizeInput(request.getId()));
        request.setRawId(SecuritySanitizer.sanitizeInput(request.getRawId()));
        request.setType(SecuritySanitizer.sanitizeInput(request.getType()));
    }

    // =============================================
    // Billing
    // =============================================

    public void sanitize(CreatePledgeBillRequest request) {
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

    public void sanitize(CreateRedemptionBillRequest request) {
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

    // =============================================
    // Customer Onboarding
    // =============================================

    public void sanitize(CustomerDto dto) {
        dto.setName(SecuritySanitizer.sanitizeInput(dto.getName()));
        dto.setStatus(SecuritySanitizer.sanitizeInput(dto.getStatus()));
        dto.setOccupation(SecuritySanitizer.sanitizeInput(dto.getOccupation()));
        dto.setImageUrl(SecuritySanitizer.sanitizeInput(dto.getImageUrl()));

        if (dto.getContacts() != null) {
            for (ContactDto c : dto.getContacts()) {
                c.setPhone(SecuritySanitizer.sanitizeInput(c.getPhone()));
                c.setSecondaryPhone(SecuritySanitizer.sanitizeInput(c.getSecondaryPhone()));
                c.setWhatsappPhone(SecuritySanitizer.sanitizeInput(c.getWhatsappPhone()));
                c.setEmail(SecuritySanitizer.sanitizeInput(c.getEmail()));
            }
        }
        if (dto.getAddresses() != null) {
            for (AddressDto a : dto.getAddresses()) {
                a.setStreet(SecuritySanitizer.sanitizeInput(a.getStreet()));
                a.setCity(SecuritySanitizer.sanitizeInput(a.getCity()));
                a.setState(SecuritySanitizer.sanitizeInput(a.getState()));
                a.setPostalCode(SecuritySanitizer.sanitizeInput(a.getPostalCode()));
                a.setCountry(SecuritySanitizer.sanitizeInput(a.getCountry()));
            }
        }
        if (dto.getIdProofs() != null) {
            for (IdProofDto p : dto.getIdProofs()) {
                p.setIdNumber(SecuritySanitizer.sanitizeInput(p.getIdNumber()));
                p.setImageUrl(SecuritySanitizer.sanitizeInput(p.getImageUrl()));
            }
        }
        if (dto.getRelatives() != null) {
            for (RelativeDto r : dto.getRelatives()) {
                r.setName(SecuritySanitizer.sanitizeInput(r.getName()));
                r.setRelationship(SecuritySanitizer.sanitizeInput(r.getRelationship()));
                r.setContactNumber(SecuritySanitizer.sanitizeInput(r.getContactNumber()));
                r.setImageUrl(SecuritySanitizer.sanitizeInput(r.getImageUrl()));
            }
        }
    }

    // =============================================
    // Pledge / Ornaments
    // =============================================

    public void sanitize(OrnamentDto dto) {
        dto.setType(SecuritySanitizer.sanitizeInput(dto.getType()));
        dto.setDescription(SecuritySanitizer.sanitizeInput(dto.getDescription()));
        dto.setImageUrl(SecuritySanitizer.sanitizeInput(dto.getImageUrl()));
    }

    // =============================================
    // Wallet
    // =============================================

    public void sanitize(WalletDepositRequest request) {
        request.setNotes(SecuritySanitizer.sanitizeInput(request.getNotes()));
        if (request.getAccounts() != null) {
            for (BillAccountResponseDto acc : request.getAccounts()) {
                acc.setAccountNumber(SecuritySanitizer.sanitizeInput(acc.getAccountNumber()));
                acc.setDirection(SecuritySanitizer.sanitizeInput(acc.getDirection()));
            }
        }
    }

    // =============================================
    // Standalone string sanitization (for @RequestParam, @PathVariable)
    // =============================================

    /**
     * Convenience pass-through for sanitizing individual string parameters.
     */
    public String sanitize(String input) {
        return SecuritySanitizer.sanitizeInput(input);
    }
}
