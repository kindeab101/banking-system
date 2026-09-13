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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void adminCanResetUserPasswordAndUserCanLoginWithNewPassword() throws Exception {
        String adminToken = token("admin", "DemoAdmin#2026");
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", "staff.toreset",
                                "email", "staff.toreset@securebank.demo",
                                "fullName", "Reset Staff",
                                "temporaryPassword", "InitialPass#2026",
                                "roleCode", "BANK_EMPLOYEE"))))
                .andExpect(status().isOk());

        String usersJson = mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var content = objectMapper.readTree(usersJson).get("content");
        long targetId = 0;
        for (var u : content) {
            if ("staff.toreset".equals(u.get("username").asText())) {
                targetId = u.get("id").asLong();
                break;
            }
        }
        assertTrue(targetId > 0);

        mockMvc.perform(post("/api/admin/users/" + targetId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newPassword", "NewEmployee#2026x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("staff.toreset", "NewEmployee#2026x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void staffCanResetCustomerPassword() throws Exception {
        String staffToken = token("employee", "DemoStaff#2026");
        String custBody = mockMvc.perform(post("/api/staff/customers")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "firstName", "Customer",
                                "lastName", "ToReset",
                                "email", "customer.toreset@securebank.demo",
                                "username", "customer.toreset",
                                "temporaryPassword", "InitialPass#2026"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long custId = objectMapper.readTree(custBody).get("id").asLong();

        mockMvc.perform(post("/api/staff/customers/" + custId + "/reset-password")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newPassword", "NewCustomer#2026x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer password reset successfully"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("customer.toreset", "NewCustomer#2026x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void adminCanFireEmployeeAndEmployeeCannotLogin() throws Exception {
        String adminToken = token("admin", "DemoAdmin#2026");
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", "staff.tofire",
                                "email", "staff.tofire@securebank.demo",
                                "fullName", "Fire Me",
                                "temporaryPassword", "DemoStaff#2026",
                                "roleCode", "BANK_EMPLOYEE"))))
                .andExpect(status().isOk());

        String usersJson = mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var content = objectMapper.readTree(usersJson).get("content");
        long targetId = 0;
        for (var u : content) {
            if ("staff.tofire".equals(u.get("username").asText())) {
                targetId = u.get("id").asLong();
                break;
            }
        }
        assertTrue(targetId > 0);

        mockMvc.perform(post("/api/admin/users/" + targetId + "/fire")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("staff.tofire", "DemoStaff#2026"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAccountWithPositiveBalanceFails() throws Exception {
        String staffToken = token("employee", "DemoStaff#2026");
        mockMvc.perform(delete("/api/staff/accounts/1000000001")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteAccountWithZeroBalanceSucceeds() throws Exception {
        String staffToken = token("employee", "DemoStaff#2026");
        String createBody = mockMvc.perform(post("/api/staff/accounts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "customerNumber", "CUS-000001",
                                "accountType", "SAVINGS",
                                "openingBalance", 0))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accountNumber = objectMapper.readTree(createBody).get("accountNumber").asText();

        mockMvc.perform(delete("/api/staff/accounts/" + accountNumber)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
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
