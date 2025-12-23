package com.moiez.pismo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.OperationType;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(TransactionController.class)
class TransactionControllerApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void shouldCreateTransaction_return200() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                1L,
                1,
                new BigDecimal("100.00")
        );

        Transaction transaction = Transaction.builder()
                .id(10L)
                .account(Account.builder()
                        .id(1L)
                        .documentNumber("123")
                        .build())
                .operationType(OperationType.fromId(2))
                .amount(new BigDecimal("100.00"))
                .build();

        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(new TransactionResponse(transaction));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(10L))
                .andExpect(jsonPath("$.transaction.account.id").value(1L))
                .andExpect(jsonPath("$.transaction.operationType").value(OperationType.fromId(2).name()))
                .andExpect(jsonPath("$.transaction.amount").value(100.00));
    }

    /* ---------------------- 404 Not Found ---------------------- */

    @Test
    void shouldReturn404_whenRelatedEntityNotFound() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                99L,
                1,
                new BigDecimal("100.00")
        );

        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new NotFoundException("Account not found"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /* ---------------------- 400 Bad Request ---------------------- */

    @Test
    void shouldReturn400_whenOperationTypeIdIsInvalid() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                1L,
                999, // invalid operationTypeId
                new BigDecimal("100.00")
        );

        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new BadRequestException("Invalid operation type"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}