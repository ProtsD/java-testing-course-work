package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.controller.AuxiliaryData.TestData;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.repository.AccountRepository;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@WithMockUser
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TestData d;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withUsername("banking")
            .withPassword("super-safe-pass");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void getUserAccount() throws Exception {
        Account randomAccount = d.findRandomAccount();
        BankingUserDetails authUser = d.getAuthUser(randomAccount.getUser().getId());

        mockMvc.perform(get("/account/{id}", randomAccount.getId())
                        .with(user(authUser)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(randomAccount.getAmount()),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name())
                );
    }

    @Test
    void depositToAccount() throws Exception {
        Account randomAccount = d.findRandomAccount();
        BankingUserDetails authUser = d.getAuthUser(randomAccount.getUser().getId());
        long depositAmount = randomAccount.getAmount() / 2;
        long expectedAmount = randomAccount.getAmount() + depositAmount;

        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", depositAmount);

        mockMvc.perform(post("/account/deposit/{id}", randomAccount.getId())
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(expectedAmount),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name()));

        long actualAmount = accountRepository.findById(randomAccount.getId()).orElseThrow().getAmount();
        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    void withdrawFromAccount() throws Exception {
        Account randomAccount = d.findRandomAccount();
        BankingUserDetails authUser = d.getAuthUser(randomAccount.getUser().getId());
        long withdrawAmount = randomAccount.getAmount() / 2;
        long expectedAmount = randomAccount.getAmount() - withdrawAmount;

        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", withdrawAmount);

        mockMvc.perform(post("/account/withdraw/{id}", randomAccount.getId())
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(expectedAmount),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name()));

        long actualAmount = accountRepository.findById(randomAccount.getId()).orElseThrow().getAmount();
        assertEquals(expectedAmount, actualAmount);
    }
}