package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.OperationType;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    private static final String IDEMP_KEY = "idem-123";

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void zeroBalance_credit_allowed() {
        Account account = createAccount(BigDecimal.ZERO);

        transactionService.createTransaction(
                credit(account.getId(), BigDecimal.valueOf(100)),
                IDEMP_KEY
        );

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(100));

        assertEquals(1, transactionRepository.count());
    }

    @Test
    void zeroBalance_debit_rejected() {
        Account account = createAccount(BigDecimal.ZERO);

        assertThrows(BadRequestException.class, () ->
                transactionService.createTransaction(
                        debit(account.getId(), BigDecimal.valueOf(50)),
                        IDEMP_KEY
                )
        );

        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertEquals(0, transactionRepository.count());
    }

    @Test
    void positiveBalance_credit_allowed() {
        Account account = createAccount(BigDecimal.valueOf(100));

        transactionService.createTransaction(
                credit(account.getId(), BigDecimal.valueOf(50)),
                IDEMP_KEY
        );

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void debit_causing_negative_balance_rejected() {
        Account account = createAccount(BigDecimal.valueOf(100));

        assertThrows(BadRequestException.class, () ->
                transactionService.createTransaction(
                        debit(account.getId(), BigDecimal.valueOf(150)),
                        IDEMP_KEY
                )
        );

        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void valid_debit_allowed() {
        Account account = createAccount(BigDecimal.valueOf(200));

        transactionService.createTransaction(
                debit(account.getId(), BigDecimal.valueOf(50)),
                IDEMP_KEY
        );

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void transaction_insert_failure_rolls_back_balance() {
        Account account = createAccount(BigDecimal.valueOf(100));

        CreateTransactionRequest bad =
                new CreateTransactionRequest(account.getId(), OperationType.PAYMENT, null);

        assertThrows(Exception.class, () ->
                transactionService.createTransaction(bad, IDEMP_KEY)
        );

        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(100));
        assertEquals(0, transactionRepository.count());
    }

    @Test
    void concurrent_transactions_are_serialized_when_using_different_idempotency_keys() throws Exception {
        Account account = createAccount(BigDecimal.valueOf(100));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable debit1 = () -> {
            try {
                transactionService.createTransaction(
                        debit(account.getId(), BigDecimal.valueOf(80)),
                        "idem-1"
                );
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        };

        Runnable debit2 = () -> {
            try {
                transactionService.createTransaction(
                        debit(account.getId(), BigDecimal.valueOf(80)),
                        "idem-2"
                );
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        };

        executor.submit(debit1);
        executor.submit(debit2);

        latch.await();

        Account updated = accountRepository.findById(account.getId()).orElseThrow();

        // Only one debit can succeed
        assertThat(updated.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(20));
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void idempotent_retry_does_not_apply_balance_twice() {
        Account account = createAccount(BigDecimal.valueOf(100));

        CreateTransactionRequest request =
                debit(account.getId(), BigDecimal.valueOf(40));

        transactionService.createTransaction(request, IDEMP_KEY);
        transactionService.createTransaction(request, IDEMP_KEY); // retry

        Account updated = accountRepository.findById(account.getId()).orElseThrow();

        assertThat(updated.getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(60));
        assertEquals(1, transactionRepository.count());
    }

    private Account createAccount(BigDecimal balance) {
        Account account = new Account();
        account.setIdempotencyKey("idem-123");
        account.setDocumentNumber("123");
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    private CreateTransactionRequest credit(Long accountId, BigDecimal amount) {
        return new CreateTransactionRequest(accountId, OperationType.PAYMENT, amount);
    }

    private CreateTransactionRequest debit(Long accountId, BigDecimal amount) {
        return new CreateTransactionRequest(accountId, OperationType.CASH_PURCHASE, amount);
    }
}
