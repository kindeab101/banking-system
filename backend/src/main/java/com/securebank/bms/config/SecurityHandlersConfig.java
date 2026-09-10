package com.securebank.bms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.bms.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.Instant;

@Configuration
public class SecurityHandlersConfig {

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, ex) -> write(mapper, response, 401, "Unauthorized", "Authentication is required");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, ex) -> write(mapper, response, 403, "Forbidden", "You are not allowed to perform this operation");
    }

    private void write(ObjectMapper mapper, HttpServletResponse response, int status, String error, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), status, error, message, ""));
    }
}
