package com.moiez.pismo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.api.dto.response.TransactionResponse;
import com.moiez.pismo.config.SecurityConfig;
import com.moiez.pismo.exception.BadRequestException;
import com.moiez.pismo.model.Account;
import com.moiez.pismo.model.OperationType;
import com.moiez.pismo.model.Transaction;
import com.moiez.pismo.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class TransactionControllerApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private static final String API_BASE_URL = "/v1/transactions";

    @Test
    void shouldCreateTransaction_return200() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                1L,
                OperationType.PAYMENT,
                new BigDecimal("100.00")
        );

        Transaction transaction = Transaction.builder()
                .id(10L)
                .account(Account.builder()
                        .id(1L)
                        .documentNumber("123")
                        .build())
                .operationType(OperationType.fromId(4))
                .amount(new BigDecimal("100.00"))
                .build();

        when(transactionService.createTransaction(any(CreateTransactionRequest.class), anyString()))
                .thenReturn(TransactionResponse.builder()
                                .transactionId(transaction.getId())
                                .accountId(transaction.getAccount().getId())
                                .amount(transaction.getAmount())
                                .operationType(transaction.getOperationType())
                                .eventTimestamp(transaction.getCreatedAt())
                                .build());

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(10L))
                .andExpect(jsonPath("$.accountId").value(1L))
                .andExpect(jsonPath("$.operationType").value(OperationType.PAYMENT.getId()))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void missing_idempotency_key_returns_400() throws Exception {
        String body = """
            {
              "accountId": 1,
              "operationType": 2,
              "amount": 100
            }
            """;

        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idempotency_key_is_forwarded_to_service() throws Exception {
        String body = """
            {
              "accountId": 1,
              "operationType": 4,
              "amount": 100
            }
            """;

        when(transactionService.createTransaction(any(), any()))
                .thenReturn(mock(TransactionResponse.class));

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(transactionService)
                .createTransaction(any(), eq("idem-123"));
    }

    @Test
    void shouldReturnBadRequest_whenRelatedEntityNotFound() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                99L,
                OperationType.WITHDRAWAL,
                new BigDecimal("100.00")
        );

        when(transactionService.createTransaction(any(CreateTransactionRequest.class), anyString()))
                .thenThrow(new BadRequestException("Invalid accountId"));

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenOperationTypeIdIsInvalid() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                1L,
                null, // invalid operationType
                new BigDecimal("100.00")
        );

        when(transactionService.createTransaction(any(CreateTransactionRequest.class), anyString()))
                .thenThrow(new BadRequestException("Invalid operation type"));

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}