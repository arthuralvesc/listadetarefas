package com.listadetarefas.security;

import com.listadetarefas.service.TokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Test
    @DisplayName("Deve autenticar com sucesso e salvar o ID numérico no contexto")
    void deveAutenticarComSucesso() throws Exception {
        request.addHeader("Authorization", "Bearer token_valido");
        when(tokenService.extrairIdDoUsuario("token_valido")).thenReturn(1L);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(1L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve passar direto se não houver cabeçalho")
    void semCabecalho() throws Exception {
        securityFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve passar direto se o tokenService retornar null (inválido)")
    void tokenInvalido() throws Exception {
        request.addHeader("Authorization", "Bearer token_lixo");
        when(tokenService.extrairIdDoUsuario("token_lixo")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
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