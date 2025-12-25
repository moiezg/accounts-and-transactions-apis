package com.moiez.pismo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.moiez.pismo.exception.BadRequestException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Arrays;

@Getter
@Schema(description = "Transaction operation type")
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

    @JsonValue
    public int getId() {
        return id;
    }

    public boolean isDebit() {
        return isDebit;
    }

    @JsonCreator
    public static OperationType fromId(Integer id) {
        return Arrays.stream(values())
                .filter(op -> op.id == id)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid operation type"));
    }
}
