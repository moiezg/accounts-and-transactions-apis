package com.moiez.pismo.model;

import com.moiez.pismo.exception.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class OperationTypeUnitTest {

    // ----------------------------------------------------
    // Valid IDs
    // ----------------------------------------------------

    @ParameterizedTest
    @EnumSource(OperationType.class)
    void fromId_shouldReturnOperationType_whenIdIsValid(OperationType operationType) {
        // when
        OperationType result = OperationType.fromId(operationType.getId());

        // then
        assertThat(result).isEqualTo(operationType);
    }

    // ----------------------------------------------------
    // Invalid IDs
    // ----------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 5, 99})
    void fromId_shouldThrowException_whenIdIsInvalid(Integer invalidId) {
        assertThatThrownBy(() -> OperationType.fromId(invalidId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid operation type");
    }
}
