package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.constant.ErrorConstants;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.ConflictingRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static com.moiez.pismo.constant.ErrorConstants.*;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String idempotencyKey) {
        log.info("Processing create account request with [Idempotency-Key: {}]", idempotencyKey);
        Optional<Account> existingAccount = repository.findByIdempotencyKey(idempotencyKey);
        if (existingAccount.isPresent()) {
            log.info("Duplicate account creation request");
            return mapToAccountResponse(existingAccount.get());
        }

        try {
            log.debug("Creating new account");
            Account account = new Account();
            account.setDocumentNumber(request.documentNumber());
            account.setIdempotencyKey(idempotencyKey);
            Account saved = repository.save(account);
            log.info("Account created successfully with ID: {}", saved.getId());
            return mapToAccountResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("An account already exists with the provided document number", e);
            throw new ConflictingRequestException(ACCOUNT_ALREADY_EXISTS);
        }
    }

    public AccountResponse getAccount(Long id) {
        log.debug("Retrieving account with ID: {}", id);
        return repository.findById(id)
                .map(this::mapToAccountResponse)
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", id);
                    return new NotFoundException(ACCOUNT_NOT_FOUND);
                });
    }

    public void applyTransaction(Long accountId, BigDecimal amount) {
        log.info("Applying transaction of amount {} to account ID: {}", amount, accountId);
        
        Account account = repository.findByIdForUpdate(accountId)
                .orElseThrow(() -> {
                    log.error("Failed to apply transaction: Account ID {} not found", accountId);
                    return new BadRequestException(ACCOUNT_NOT_FOUND);
                });

        BigDecimal updatedBalance = amount.add(account.getBalance());
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Insufficient funds for account ID: {}. Current balance: {}, Attempted debit: {}", 
                    accountId, account.getBalance(), amount);
            throw new BadRequestException(INSUFFICIENT_FUNDS);
        }

        account.setBalance(updatedBalance);
        log.debug("Transaction applied successfully. New balance for account {}: {}", accountId, updatedBalance);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .documentNumber(account.getDocumentNumber())
                .balance(account.getBalance())
                .build();
    }
}
