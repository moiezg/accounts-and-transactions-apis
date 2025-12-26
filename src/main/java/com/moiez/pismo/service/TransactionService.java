package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.constant.ApiConstants;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public TransactionResponse createTransaction(
            CreateTransactionRequest request,
            String idempotencyKey
    ) {
        log.info("Processing transaction request for account: {} [{}: {}]", 
                request.accountId(), ApiConstants.IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent()) {
            log.info("Transaction already processed with key: {}", idempotencyKey);
            return mapToTransactionResponse(existingTransaction.get());
        }

        log.debug("Creating new transaction for account: {}", request.accountId());

        BigDecimal finalAmount = request.operationType().isDebit()
                ? request.amount().negate()
                : request.amount();

        accountService.applyTransaction(request.accountId(), finalAmount);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .account(Account.builder().id(request.accountId()).build())
                .operationType(request.operationType())
                .amount(finalAmount)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created successfully with ID: {}", saved.getId());

        return mapToTransactionResponse(saved);
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .amount(transaction.getAmount())
                .operationType(transaction.getOperationType())
                .eventTimestamp(transaction.getCreatedAt())
                .build();
    }
}
