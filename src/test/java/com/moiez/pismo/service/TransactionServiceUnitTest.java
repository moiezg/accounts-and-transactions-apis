package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.OperationType;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long ACCOUNT_ID = 1L;

    @Test
    void credit_transaction_is_created_successfully() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.PAYMENT, // credit
                        BigDecimal.valueOf(100)
                );

        Transaction savedTransaction = Transaction.builder()
                .id(10L)
                .account(Account.builder().id(ACCOUNT_ID).build())
                .operationType(OperationType.PAYMENT)
                .amount(BigDecimal.valueOf(100))
                .createdAt(Instant.now())
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        TransactionResponse response =
                transactionService.createTransaction(request);

        verify(accountService).applyTransaction(
                ACCOUNT_ID,
                BigDecimal.valueOf(100)
        );

        verify(transactionRepository).save(any(Transaction.class));

        assertEquals(10L, response.transactionId());
        assertEquals(ACCOUNT_ID, response.accountId());
        assertEquals(
                0,
                response.amount().compareTo(BigDecimal.valueOf(100))
        );
        assertEquals(OperationType.PAYMENT, response.operationType());
        assertNotNull(response.eventTimestamp());
    }

    @Test
    void debit_transaction_negates_amount() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.WITHDRAWAL, // debit
                        BigDecimal.valueOf(50)
                );

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(20L);
                    return tx;
                });

        transactionService.createTransaction(request);

        verify(accountService).applyTransaction(
                ACCOUNT_ID,
                BigDecimal.valueOf(-50)
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals(
                0,
                saved.getAmount().compareTo(BigDecimal.valueOf(-50))
        );
    }

    @Test
    void account_service_exception_is_propagated() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.WITHDRAWAL,
                        BigDecimal.valueOf(100)
                );

        doThrow(new BadRequestException("Insufficient funds"))
                .when(accountService)
                .applyTransaction(anyLong(), any());

        assertThrows(BadRequestException.class, () ->
                transactionService.createTransaction(request)
        );

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void repository_failure_is_propagated() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.PAYMENT,
                        BigDecimal.valueOf(100)
                );

        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class, () ->
                transactionService.createTransaction(request)
        );

        verify(accountService).applyTransaction(
                ACCOUNT_ID,
                BigDecimal.valueOf(100)
        );
    }

    @Test
    void transaction_references_account_by_id_only() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.PAYMENT,
                        BigDecimal.TEN
                );

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction tx = captor.getValue();
        assertEquals(ACCOUNT_ID, tx.getAccount().getId());
    }

    @Test
    void zero_amount_is_passed_through() {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        ACCOUNT_ID,
                        OperationType.PAYMENT,
                        BigDecimal.ZERO
                );

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        verify(accountService).applyTransaction(
                ACCOUNT_ID,
                BigDecimal.ZERO
        );
    }
}
