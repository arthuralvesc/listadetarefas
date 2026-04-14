package com.listadetarefas.usuarios.security;

import com.listadetarefas.usuarios.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Cenários de Sucesso")
    class CenariosDeSucesso {

        @Test
        @DisplayName("Deve autenticar o usuário quando o token for válido")
        void deveAutenticarComTokenValido() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer meu_token_jwt_valido");
            when(tokenService.validarToken("meu_token_jwt_valido")).thenReturn("arthur@exemplo.com");

            securityFilter.doFilterInternal(request, response, filterChain);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());

            assertEquals("arthur@exemplo.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Cenários de Rejeição/Bypass")
    class CenariosDeRejeicao {

        @Test
        @DisplayName("Não deve autenticar quando o cabeçalho Authorization não existir")
        void devePassarDiretoSemToken() throws ServletException, IOException {

            securityFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verify(filterChain, times(1)).doFilter(request, response);

            verify(tokenService, never()).validarToken(anyString());
        }

        @Test
        @DisplayName("Não deve autenticar quando o token for inválido")
        void devePassarDiretoComTokenInvalido() throws ServletException, IOException {

            request.addHeader("Authorization", "Bearer token_hackeado");
            when(tokenService.validarToken("token_hackeado")).thenReturn(""); // Retorna vazio

            securityFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Cobertura Técnica - @NonNull")
    class CoberturaNonNull {

        @Test
        @DisplayName("Deve falhar quando o Request for nulo")
        void deveFalharQuandoRequestForNulo() {
            assertThrows(NullPointerException.class, () -> {
                securityFilter.doFilterInternal(null, response, filterChain);
            });
        }

        @Test
        @DisplayName("Deve falhar quando o Response for nulo")
        void deveFalharQuandoResponseForNulo() {
            assertThrows(NullPointerException.class, () -> {
                securityFilter.doFilterInternal(request, null, filterChain);
            });
        }

        @Test
        @DisplayName("Deve falhar quando o FilterChain for nulo")
        void deveFalharQuandoFilterChainForNulo() {
            assertThrows(NullPointerException.class, () -> {
                securityFilter.doFilterInternal(request, response, null);
            });
        }
    }
}