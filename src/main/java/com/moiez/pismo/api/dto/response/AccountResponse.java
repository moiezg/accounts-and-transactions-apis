package com.moiez.pismo.api.dto.response;

import lombok.Builder;

import java.math.BigDecimal;


@Builder
public record AccountResponse(
        Long id,
        String documentNumber,
        BigDecimal balance
) {}