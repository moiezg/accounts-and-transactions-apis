package com.moiez.pismo.api.controller;

import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.constant.ApiConstants;
import com.moiez.pismo.constant.ErrorConstants;
import com.moiez.pismo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping(ApiConstants.ACCOUNTS_BASE_URL)
@Tag(name = "Accounts", description = "Account APIs")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create an account")
    public ResponseEntity<AccountResponse> create(
            @RequestHeader(ApiConstants.IDEMPOTENCY_KEY_HEADER) 
            @NotBlank(message = ErrorConstants.IDEMPOTENCY_KEY_REQUIRED) String idempotencyKey,
            @RequestBody @Valid CreateAccountRequest request) {
        log.info("Received request to create account with document: {} [{}: {}]",
                request.documentNumber(), ApiConstants.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        
        AccountResponse response = service.createAccount(request, idempotencyKey);
        
        log.info("Account created successfully with ID: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details")
    public ResponseEntity<AccountResponse> get(
            @PathVariable("id")
            @Parameter(description = "Account ID")
            Long id) {
        log.debug("Fetching account details for ID: {}", id);
        return ResponseEntity.ok(service.getAccount(id));
    }
}
