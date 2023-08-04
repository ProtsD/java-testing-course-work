package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.controller.AuxiliaryData.TestData;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
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
    @Transactional
    void createUser(@Value("${app.security.admin-token}") String adminToken) throws Exception {
        JSONObject newUserJson = d.generateNewUser();

        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson.toString()))
                .andExpect(status().isOk());

        assertTrue(userRepository.findByUsername(newUserJson.getAsString("username")).isPresent());

        accountRepository.findAll().forEach(System.out::println);
    }


    @Test
    @WithMockUser
    void getAllUsers() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void getMyProfile() throws Exception {
        User user = userRepository.findAll().stream().findFirst().orElseThrow();
        BankingUserDetails authUser = new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);

        mockMvc.perform(get("/user/me")
                        .with(user(authUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty());
    }
}