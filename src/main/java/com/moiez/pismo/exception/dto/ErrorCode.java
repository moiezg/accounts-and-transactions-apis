package com.moiez.pismo.exception.dto;

public enum ErrorCode {

    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND"),
    INVALID_OPERATION_TYPE("INVALID_OPERATION_TYPE"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
