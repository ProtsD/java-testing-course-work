package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.skypro.simplebanking.controller.AuxiliaryData.TestData;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.validation.constraints.AssertTrue;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;
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
    void transfer() throws Exception {
        List<Account> twoRandomAccounts = d.findTwoRandomAccounts();
        TransferRequest transferRequest = d.createTransferRequest(twoRandomAccounts.get(0), twoRandomAccounts.get(1));
        String json = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(transferRequest);

        User user = userRepository.findById(twoRandomAccounts.get(0).getUser().getId()).orElseThrow();
        BankingUserDetails authUser = new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(authUser)))
                .andExpect(status().isOk());

        assertEquals(transferRequest.getAmount(), twoRandomAccounts.get(0).getAmount() - accountRepository.findById(twoRandomAccounts.get(0).getId()).orElseThrow().getAmount());
        assertEquals(transferRequest.getAmount(), accountRepository.findById(twoRandomAccounts.get(1).getId()).orElseThrow().getAmount() - twoRandomAccounts.get(1).getAmount());
    }
}