package com.moiez.pismo.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "Document number is required")
        String documentNumber)
{}
