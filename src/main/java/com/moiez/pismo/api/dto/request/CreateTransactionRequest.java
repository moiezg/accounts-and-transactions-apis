package com.moiez.pismo.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest (

    @NotNull(message = "Account Id is required")
    Long accountId,

    @NotNull(message = "operationTypeId is required")
    Integer operationTypeId,

    @NotNull(message = "Transaction amount is required")
    BigDecimal amount
) {}