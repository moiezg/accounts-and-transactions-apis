package com.moiez.pismo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.config.SecurityConfig;
import com.moiez.pismo.constant.ApiConstants;
import com.moiez.pismo.service.AccountService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AccountControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createAccount_shouldReturn201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("12345678900");
        AccountResponse response = new AccountResponse(1L, "12345678900", BigDecimal.ZERO);

        when(accountService.createAccount(any(CreateAccountRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post(ApiConstants.ACCOUNTS_BASE_URL)
                        .header(ApiConstants.IDEMPOTENCY_KEY_HEADER, "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.documentNumber").value("12345678900"));
    }

    @Test
    void createAccount_withoutIdempotencyKey_shouldReturn400() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("12345678900");

        mockMvc.perform(post(ApiConstants.ACCOUNTS_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccount_shouldReturn200() throws Exception {
        AccountResponse response = new AccountResponse(1L, "12345678900", BigDecimal.TEN);

        when(accountService.getAccount(1L)).thenReturn(response);

        mockMvc.perform(get(ApiConstants.ACCOUNTS_BASE_URL + "/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.balance").value(10.00));
    }

    @Test
    void createAccount_invalidDocument_shouldReturn400() throws Exception {
        // Empty document number
        String body = "{\"documentNumber\": \"\"}";

        mockMvc.perform(post(ApiConstants.ACCOUNTS_BASE_URL)
                        .header(ApiConstants.IDEMPOTENCY_KEY_HEADER, "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}