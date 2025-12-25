package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {

    @Mock
    private AccountRepository repository;

    @InjectMocks
    private AccountService service;

    private static final String DOCUMENT_NUMBER = "12345678900";
    private static final Long ACCOUNT_ID = 1L;

    // ----------------------------------------------------
    // createAccount
    // ----------------------------------------------------

    @Test
    void createAccount_shouldCreateAccount_whenAccountDoesNotExist() {
        // given
        CreateAccountRequest request = new CreateAccountRequest(DOCUMENT_NUMBER);

        Account savedAccount = Account.builder()
                .id(ACCOUNT_ID)
                .documentNumber(DOCUMENT_NUMBER)
                .build();

        when(repository.existsByDocumentNumber(DOCUMENT_NUMBER)).thenReturn(false);
        when(repository.save(any(Account.class))).thenReturn(savedAccount);

        // when
        AccountResponse response = service.createAccount(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ACCOUNT_ID);
        assertThat(response.documentNumber()).isEqualTo(DOCUMENT_NUMBER);

        verify(repository).existsByDocumentNumber(DOCUMENT_NUMBER);
        verify(repository).save(any(Account.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createAccount_shouldThrowBadRequestException_whenAccountAlreadyExists() {
        // given
        CreateAccountRequest request = new CreateAccountRequest(DOCUMENT_NUMBER);

        when(repository.existsByDocumentNumber(DOCUMENT_NUMBER)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> service.createAccount(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Account already exists");

        verify(repository).existsByDocumentNumber(DOCUMENT_NUMBER);
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository);
    }

    // ----------------------------------------------------
    // getAccount
    // ----------------------------------------------------

    @Test
    void getAccount_shouldReturnAccount_whenAccountExists() {
        // given
        Account account = Account.builder()
                .id(ACCOUNT_ID)
                .documentNumber(DOCUMENT_NUMBER)
                .build();

        when(repository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        // when
        AccountResponse response = service.getAccount(ACCOUNT_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ACCOUNT_ID);
        assertThat(response.documentNumber()).isEqualTo(DOCUMENT_NUMBER);

        verify(repository).findById(ACCOUNT_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getAccount_shouldThrowNotFoundException_whenAccountDoesNotExist() {
        // given
        when(repository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.getAccount(ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");

        verify(repository).findById(ACCOUNT_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void credit_increases_balance() {
        Account account = new Account();
        account.setBalance(BigDecimal.valueOf(100));

        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        service.applyTransaction(1L, BigDecimal.valueOf(50));

        assertEquals(0,
                account.getBalance().compareTo(BigDecimal.valueOf(150))
        );
    }

    @Test
    void debit_decreases_balance() {
        Account account = new Account();
        account.setBalance(BigDecimal.valueOf(100));

        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        service.applyTransaction(1L, BigDecimal.valueOf(-30));

        assertEquals(0,
                account.getBalance().compareTo(BigDecimal.valueOf(70))
        );
    }

    @Test
    void debit_causing_negative_balance_throws() {
        Account account = new Account();
        account.setBalance(BigDecimal.valueOf(50));

        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class, () ->
                service.applyTransaction(1L, BigDecimal.valueOf(-100))
        );

        // balance unchanged
        assertEquals(0,
                account.getBalance().compareTo(BigDecimal.valueOf(50))
        );
    }

    @Test
    void invalid_account_throws() {
        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                service.applyTransaction(1L, BigDecimal.TEN)
        );
    }
}
