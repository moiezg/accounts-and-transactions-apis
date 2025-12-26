package com.moiez.pismo.api.controller;

import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/transactions")
@Tag(name = "Transactions", description = "Transaction APIs")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<TransactionResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestBody @Valid CreateTransactionRequest request) {
        log.info("Received transaction request for account: {} type: {} amount: {} [Idempotency-Key: {}]", 
                request.accountId(), request.operationType(), request.amount(), idempotencyKey);
        
        TransactionResponse response = service.createTransaction(request, idempotencyKey);
        
        log.info("Transaction created successfully with ID: {}", response.transactionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}
