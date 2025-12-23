package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private static final String DOCUMENT_NUMBER = "12345678900";

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        accountRepository.deleteAll();
    }

    // ----------------------------------------------------
    // CREATE
    // ----------------------------------------------------

    @Test
    void createAccount_shouldPersistAccount_whenNotExists() {
        // when
        AccountResponse response =
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));

        // then
        assertThat(response).isNotNull();
        assertThat(response.account().getId()).isNotNull();
        assertThat(response.account().getDocumentNumber()).isEqualTo(DOCUMENT_NUMBER);

        List<Account> accounts = accountRepository.findAll();
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getDocumentNumber())
                .isEqualTo(DOCUMENT_NUMBER);
    }

    @Test
    void createAccount_shouldThrowException_andNotInsert_whenAccountExists() {
        // given
        accountService.createAccount(
                new CreateAccountRequest(DOCUMENT_NUMBER));

        // when / then
        assertThatThrownBy(() ->
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Account already exists");

        // ensure no second insert
        assertThat(accountRepository.findAll())
                .hasSize(1);
    }

    @Test
    void createAccount_shouldAllowOnlyOneInsert_underConcurrentRequests()
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));
            } catch (Exception ignored) {
                // expected for one thread
            } finally {
                latch.countDown();
            }
        };

        // when
        executor.submit(task);
        executor.submit(task);
        latch.await();

        // then
        List<Account> accounts = accountRepository.findAll();
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getDocumentNumber())
                .isEqualTo(DOCUMENT_NUMBER);
    }

    // ----------------------------------------------------
    // GET
    // ----------------------------------------------------

    @Test
    void getAccount_shouldReturnAccount_whenExists() {
        // given
        AccountResponse created =
                accountService.createAccount(
                        new CreateAccountRequest(DOCUMENT_NUMBER));

        // when
        AccountResponse fetched =
                accountService.getAccount(created.account().getId());

        // then
        assertThat(fetched).isNotNull();
        assertThat(fetched.account().getId()).isEqualTo(created.account().getId());
        assertThat(fetched.account().getDocumentNumber())
                .isEqualTo(DOCUMENT_NUMBER);
    }

    @Test
    void getAccount_shouldThrowException_whenNotFound() {
        // when / then
        assertThatThrownBy(() ->
                accountService.getAccount(9999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");
    }
}
