package com.moiez.pismo.model;

import com.moiez.pismo.exception.BadRequestException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OperationType {

    CASH_PURCHASE(1, true),
    INSTALLMENT_PURCHASE(2, true),
    WITHDRAWAL(3, true),
    PAYMENT(4, false);

    private final int id;
    private final boolean isDebit;

    OperationType(int id, boolean isDebit) {
        this.id = id;
        this.isDebit = isDebit;
    }

    public static OperationType fromId(Integer id) {
        return Arrays.stream(values())
                .filter(op -> op.id == id)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid operation type"));
    }
}
