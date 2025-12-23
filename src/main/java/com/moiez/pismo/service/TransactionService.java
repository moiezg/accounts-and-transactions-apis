package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.OperationType;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {

        Account account = accountService.getAccount(request.accountId()).account();
        OperationType operationType = OperationType.fromId(request.operationTypeId());

        BigDecimal finalAmount = getFinalAmount(request.amount(), operationType);

        Transaction transaction = Transaction.builder()
                .account(account)
                .operationType(operationType)
                .amount(finalAmount)
                .eventDate(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        return new TransactionResponse(transaction);
    }

    private BigDecimal getFinalAmount(BigDecimal amount, OperationType operationType) {
        return operationType.isDebit()
                ? amount.negate()
                : amount;
    }
}