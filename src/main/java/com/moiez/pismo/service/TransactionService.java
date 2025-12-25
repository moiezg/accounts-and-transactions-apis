package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        BigDecimal finalAmount = request.operationType().isDebit()
                ? request.amount().negate()
                : request.amount();

        accountService.applyTransaction(request.accountId(), finalAmount);

        Transaction transaction = Transaction.builder()
                .account(Account.builder().id(request.accountId()).build())
                .operationType(request.operationType())
                .amount(finalAmount)
                .createdAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);

        return mapToTransactionResponse(transaction);
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