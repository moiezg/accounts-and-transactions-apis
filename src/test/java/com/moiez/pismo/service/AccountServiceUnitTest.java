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

import static com.moiez.pismo.constant.ErrorConstants.ACCOUNT_NOT_FOUND;
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
    private static final String IDEMP_KEY = "idem-123";

    @Test
    void createAccount_shouldCreateAccount_whenAccountDoesNotExist() {
        // given
        CreateAccountRequest request = new CreateAccountRequest(DOCUMENT_NUMBER);

        Account savedAccount = Account.builder()
                .id(ACCOUNT_ID)
                .documentNumber(DOCUMENT_NUMBER)
                .build();

        when(repository.save(any(Account.class))).thenReturn(savedAccount);

        // when
        AccountResponse response = service.createAccount(request, IDEMP_KEY);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ACCOUNT_ID);
        assertThat(response.documentNumber()).isEqualTo(DOCUMENT_NUMBER);
    }

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
                .hasMessage(ACCOUNT_NOT_FOUND);

        verify(repository).findById(ACCOUNT_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void applyTransaction_invalid_account_throws() {
        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                service.applyTransaction(1L, BigDecimal.TEN)
        );
    }
}
