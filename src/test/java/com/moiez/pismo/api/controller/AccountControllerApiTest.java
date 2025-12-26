package com.moiez.pismo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moiez.pismo.api.dto.request.CreateAccountRequest;
import com.moiez.pismo.api.dto.response.AccountResponse;
import com.moiez.pismo.exception.ConflictingRequestException;
import com.moiez.pismo.exception.NotFoundException;
import com.moiez.pismo.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
public class AccountControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;
    
    private static final String IDEMP_KEY = "idemp-123";
    private static final String API_BASE_URL = "/v1/accounts";

    @Test
    void missing_idempotency_key_returns_400() throws Exception {
        String body = """
            {
              "documentNumber": "123"
            }
            """;

        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateAccount_return201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("123");

        when(accountService.createAccount(any(CreateAccountRequest.class), eq(IDEMP_KEY)))
                .thenReturn(AccountResponse.builder()
                                .id(1L)
                                .documentNumber("123")
                                .build());

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-key", IDEMP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.documentNumber").value("123"));
    }

    @Test
    void shouldReturn409_whenAccountAlreadyExists() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("123");

        when(accountService.createAccount(any(CreateAccountRequest.class), eq(IDEMP_KEY)))
                .thenThrow(new ConflictingRequestException("Account already exists"));

        mockMvc.perform(post(API_BASE_URL)
                        .header("Idempotency-key", IDEMP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetAccount_return200() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(AccountResponse.builder()
                .id(1L)
                .documentNumber("12345678900")
                .build());

        mockMvc.perform(get(API_BASE_URL + "/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.documentNumber").value("12345678900"));
    }

    @Test
    void shouldReturn404_whenAccountNotFound() throws Exception {
        when(accountService.getAccount(99L))
                .thenThrow(new NotFoundException("Account not found"));

        mockMvc.perform(get(API_BASE_URL + "/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}