package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.OperationType;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.repository.AccountRepository;
import com.moiez.pismo.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private static final String DOCUMENT_NUMBER = "99999999999";

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    // ----------------------------------------------------
    // CREATE TRANSACTION
    // ----------------------------------------------------

    @Test
    void createTransaction_shouldCreateDebitTransaction_withNegativeAmount() {
        // given
        AccountResponse accountResponse =
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));

        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        accountResponse.account().getId(),
                        OperationType.CASH_PURCHASE.getId(), // debit
                        new BigDecimal("100.00")
                );

        // when
        TransactionResponse response =
                transactionService.createTransaction(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.transaction().getAmount())
                .isEqualByComparingTo("-100.00");

        List<Transaction> transactions =
                transactionRepository.findAll();

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount())
                .isEqualByComparingTo("-100.00");
    }

    @Test
    void createTransaction_shouldCreateCreditTransaction_withPositiveAmount() {
        // given
        AccountResponse accountResponse =
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));

        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        accountResponse.account().getId(),
                        OperationType.PAYMENT.getId(), // credit
                        new BigDecimal("250.00")
                );

        // when
        TransactionResponse response =
                transactionService.createTransaction(request);

        // then
        assertThat(response.transaction().getAmount())
                .isEqualByComparingTo("250.00");

        Transaction transaction =
                transactionRepository.findAll().get(0);

        assertThat(transaction.getOperationType())
                .isEqualTo(OperationType.PAYMENT);
    }

    @Test
    void createTransaction_shouldThrowException_whenOperationTypeIsInvalid() {
        // given
        AccountResponse account =
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));

        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        account.account().getId(),
                        999, // invalid operation type
                        new BigDecimal("50.00")
                );

        // when / then
        assertThatThrownBy(() ->
                transactionService.createTransaction(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid operation type");

        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void createTransaction_shouldThrowException_whenAccountDoesNotExist() {
        // given
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        9999L,
                        OperationType.CASH_PURCHASE.getId(),
                        new BigDecimal("75.00")
                );

        // when / then
        assertThatThrownBy(() ->
                transactionService.createTransaction(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");

        assertThat(transactionRepository.findAll()).isEmpty();
    }
}
