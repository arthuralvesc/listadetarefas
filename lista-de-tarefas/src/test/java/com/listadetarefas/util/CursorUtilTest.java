package com.listadetarefas.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CursorUtilTest {

    private CursorUtil cursorUtil;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        cursorUtil = new CursorUtil(objectMapper);
    }

    @Test
    @DisplayName("Deve codificar mapa de chaves para Base64 com sucesso")
    void codificarCursor() {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("id", 100L);
        keys.put("dataCriacao", "2026-05-15T19:38:59.228");

        String base64 = cursorUtil.codificarCursor(keys);

        assertNotNull(base64);
        assertFalse(base64.isBlank());
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando Jackson falhar ao codificar json")
    void codificarCursorLancaException() throws JsonProcessingException {
        ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
        CursorUtil utilComMock = new CursorUtil(mockMapper);

        Mockito.when(mockMapper.writeValueAsString(Mockito.any()))
                .thenThrow(Mockito.mock(JsonProcessingException.class));

        Map<String, Object> keys = Map.of("id", 1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> utilComMock.codificarCursor(keys));
        assertEquals("Erro ao codificar o cursor de paginação", exception.getMessage());
    }

    @Test
    @DisplayName("Deve decodificar cursor convertendo dataCriacao para LocalDateTime")
    void decodificarCursorComDataCriacaoConvertida() {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("dataCriacao", "2026-05-15T19:38:59.228");
        keys.put("id", 105);

        String base64 = cursorUtil.codificarCursor(keys);
        Map<String, Object> decodificado = cursorUtil.decodificarCursor(base64);

        assertInstanceOf(LocalDateTime.class, decodificado.get("dataCriacao"));
        assertEquals(105, decodificado.get("id"));
    }

    @Test
    @DisplayName("Deve decodificar cursor normalmente ignorando o cast quando não houver dataCriacao")
    void decodificarCursorSemDataCriacao() {
        Map<String, Object> keys = Map.of("id", 999);
        String base64 = cursorUtil.codificarCursor(keys);

        Map<String, Object> decodificado = cursorUtil.decodificarCursor(base64);

        assertFalse(decodificado.containsKey("dataCriacao"));
        assertEquals(999, decodificado.get("id"));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para Base64 inválido")
    void decodificarCursorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> cursorUtil.decodificarCursor("dGV4dG9faW52YWxpZG8="));
    }

    @Test
    @DisplayName("Deve retornar KeysetScrollPosition no início se base64 for nulo ou vazio")
    void resolverScrollPositionNuloOuVazio() {
        assertInstanceOf(KeysetScrollPosition.class, cursorUtil.resolverScrollPosition(null));
        assertInstanceOf(KeysetScrollPosition.class, cursorUtil.resolverScrollPosition("   "));
    }

    @Test
    @DisplayName("Deve retornar KeysetScrollPosition com valores decodificados se base64 for válido")
    void resolverScrollPositionValido() {
        Map<String, Object> keys = Map.of("id", 1L);
        String base64 = cursorUtil.codificarCursor(keys);

        ScrollPosition position = cursorUtil.resolverScrollPosition(base64);

        assertNotNull(position);
        assertInstanceOf(KeysetScrollPosition.class, position);
    }

    @Test
    @DisplayName("Deve ignorar conversão se dataCriacao existir mas não for do tipo String")
    void decodificarCursorComDataCriacaoNaoString() {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("dataCriacao", 123456789L);
        keys.put("id", 100);

        String base64 = cursorUtil.codificarCursor(keys);

        Map<String, Object> decodificado = cursorUtil.decodificarCursor(base64);

        assertTrue(decodificado.containsKey("dataCriacao"));

        assertFalse(decodificado.get("dataCriacao") instanceof LocalDateTime);
        assertNotNull(decodificado.get("id"));
    }
}