package com.moiez.pismo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.request.CreateTransactionRequest;
import com.moiez.pismo.repository.AccountRepository;
import com.moiez.pismo.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PismoE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    // ----------------------------------------------------
    // E2E TEST 1 — HAPPY PATH
    // ----------------------------------------------------

    @Test
    void e2e_createAccount_createTransaction_getAccount_success() throws Exception {
        // 1️⃣ Create Account
        CreateAccountRequest createAccount =
                new CreateAccountRequest("11122233344");

        String accountResponseJson =
                mockMvc.perform(post("/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createAccount)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.account.id").exists())
                        .andExpect(jsonPath("$.account.documentNumber")
                                .value("11122233344"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long accountId =
                objectMapper.readTree(accountResponseJson)
                        .get("account")
                        .get("id")
                        .asLong();

        // 2️⃣ Create Transaction
        CreateTransactionRequest createTransaction =
                new CreateTransactionRequest(
                        accountId,
                        1, // PURCHASE
                        new BigDecimal("100.00")
                );

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.amount").value(-100.00));

        // 3️⃣ Get Account
        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(accountId))
                .andExpect(jsonPath("$.account.documentNumber")
                        .value("11122233344"));

        // DB sanity check
        assertThat(accountRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    // ----------------------------------------------------
    // E2E TEST 2 — FAILURE PATH
    // ----------------------------------------------------

    @Test
    void e2e_createTransaction_withInvalidOperationType_shouldReturn400() throws Exception {
        // given — account exists
        String accountJson =
                mockMvc.perform(post("/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "documentNumber": "55566677788"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long accountId =
                objectMapper.readTree(accountJson)
                        .get("account")
                        .get("id")
                        .asLong();

        // when — invalid operationTypeId
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_id": %d,
                                  "operation_type_id": 999,
                                  "amount": 50.00
                                }
                                """.formatted(accountId)))
                .andExpect(status().isBadRequest());

        // then — no transaction inserted
        assertThat(transactionRepository.count()).isZero();
    }
}
