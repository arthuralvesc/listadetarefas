package com.listadetarefas.usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.usuarios.dto.LoginRequestDTO;
import com.listadetarefas.usuarios.security.SecurityFilter;
import com.listadetarefas.usuarios.service.AuthService;
import com.listadetarefas.usuarios.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityFilter securityFilter;

    @Test
    @DisplayName("Deve retornar status 200 e o Token quando credenciais forem válidas")
    void deveRetornar200QuandoLoginValido() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("arthur@exemplo.com", "senha123");

        when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn("meu_token_super_secreto");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))) // Converte o DTO em string JSON

                .andExpect(status().isOk())
                .andExpect(content().string("meu_token_super_secreto"));
    }

    @Test
    @DisplayName("Deve retornar status 401 (Unauthorized) quando a Service lançar exceção")
    void deveRetornar401QuandoLoginInvalido() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("arthur@exemplo.com", "senhaerrada");

        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Senha incorreta"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))

                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Senha incorreta"));
    }
}