package com.listadetarefas.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeignConfigTest {

    private RequestInterceptor interceptor;
    private RequestTemplate requestTemplate;
    private HttpServletRequest requestMock;

    @BeforeEach
    void setup() {
        FeignConfig feignConfig = new FeignConfig();
        interceptor = feignConfig.requestInterceptor();

        requestTemplate = new RequestTemplate();

        requestMock = mock(HttpServletRequest.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Cenário Feliz: Deve propagar o Header Authorization para o Feign")
    void devePropagarHeaderComSucesso() {
        String tokenValido = "Bearer eyJhbGci...";
        when(requestMock.getHeader("Authorization")).thenReturn(tokenValido);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));

        interceptor.apply(requestTemplate);

        assertTrue(requestTemplate.headers().containsKey("Authorization"));
        assertTrue(requestTemplate.headers().get("Authorization").contains(tokenValido));
    }

    @Test
    @DisplayName("Cenário de Falha 1: Deve estourar exceção se não estiver em um contexto Web")
    void deveFalharSeContextoWebNaoExistir() {
        RequestContextHolder.resetRequestAttributes();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            interceptor.apply(requestTemplate);
        });

        assertTrue(exception.getMessage().contains("Contexto de requisição web não encontrado"));
    }

    @Test
    @DisplayName("Cenário de Falha 2: Deve estourar exceção se o Header Authorization for nulo ou vazio")
    void deveFalharSeHeaderEstiverVazio() {
        when(requestMock.getHeader("Authorization")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            interceptor.apply(requestTemplate);
        });

        assertTrue(exception.getMessage().contains("ausente ou vazio na requisição de origem"));
    }

    @Test
    @DisplayName("Cenário de Falha 3: Deve estourar exceção se o Header Authorization for apenas espaços (isBlank)")
    void deveFalharSeHeaderForApenasEspacos() {
        when(requestMock.getHeader("Authorization")).thenReturn("     ");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            interceptor.apply(requestTemplate);
        });

        assertTrue(exception.getMessage().contains("ausente ou vazio na requisição de origem"));
    }
}
