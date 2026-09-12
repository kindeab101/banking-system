package com.securebank.bms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.bms.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void loginWithValidDemoCustomer() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("customer.a", "DemoCustomer#2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordIsGenericUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("customer.a", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void customerCannotCallAdminUsers() throws Exception {
        String token = token("customer.a", "DemoCustomer#2026");
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAnotherAccount() throws Exception {
        String token = token("customer.a", "DemoCustomer#2026");
        mockMvc.perform(get("/api/accounts/1000000003").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedIs401() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatedCustomerCanLoginAndSeeAccount() throws Exception {
        String adminToken = token("admin", "DemoAdmin#2026");
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", "customer.lina",
                                "email", "lina.hailu@securebank.demo",
                                "fullName", "Lina Hailu",
                                "temporaryPassword", "DemoTemp#2026x",
                                "roleCode", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("customer.lina"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));

        String customerToken = token("customer.lina", "DemoTemp#2026x");
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").isNotEmpty())
                .andExpect(jsonPath("$[0].accountType").value("SAVINGS"));
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lina Hailu"));
    }

    private String token(String user, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, password))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
