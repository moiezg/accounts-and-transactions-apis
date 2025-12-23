package com.moiez.pismo.service;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        String documentNumber = createAccountRequest.documentNumber();
        if (repository.existsByDocumentNumber(documentNumber)) {
            throw new BadRequestException("Account already exists");
        }

        Account account = Account.builder()
                .documentNumber(documentNumber)
                .build();
        account = repository.save(account);

        return new AccountResponse(account);
    }

    public AccountResponse getAccount(Long id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return new AccountResponse(account);
    }
}