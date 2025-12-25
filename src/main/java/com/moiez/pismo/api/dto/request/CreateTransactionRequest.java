package com.moiez.pismo.api.dto.request;

import com.moiez.pismo.model.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(
        name = "Transaction Request",
        description = "Represents the create request for a transaction"
)
public record CreateTransactionRequest (

    @Schema(
            description = "Unique account identifier",
            example = "123"
    )
    @NotNull(message = "Account Id is required")
    Long accountId,

    @Schema(
            description = "Type of transaction operation",
            example = "1",
            allowableValues = {"1", "2", "3", "4"}
    )
    @NotNull(message = "operation type is required")
    OperationType operationType,

    @Schema(
            description = "Transaction amount",
            example = "123.45",
            multipleOf = 0.01
    )
    @NotNull(message = "Transaction amount is required")
    @Digits(integer = 12, fraction = 2,
            message = "Transaction amount must have at most 2 decimal places")
    BigDecimal amount
) {}