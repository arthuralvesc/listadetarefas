package com.listadetarefas.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class CursorUtil {

    private final ObjectMapper objectMapper;

    public CursorUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Transforma as chaves do Keyset em uma String Base64 opaca.
     */
    public String codificarCursor(Map<String, Object> keys) {
        try {
            String json = objectMapper.writeValueAsString(keys);

            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao codificar o cursor de paginação", e);
        }
    }

    /**
     * Decodifica a String Base64 de volta para um Map de chaves.
     */
    public Map<String, Object> decodificarCursor(String base64) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);

            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            if (map.containsKey("dataCriacao") && map.get("dataCriacao") instanceof String strData) {
                map.put("dataCriacao", java.time.LocalDateTime.parse(strData));
            }

            return map;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cursor de paginação inválido", e);
        }
    }

    public ScrollPosition resolverScrollPosition(String cursorBase64) {
        if (cursorBase64 != null && !cursorBase64.isBlank()) {
            Map<String, Object> cursorValues = decodificarCursor(cursorBase64);
            return ScrollPosition.of(cursorValues, ScrollPosition.Direction.FORWARD);
        }
        return ScrollPosition.keyset();
    }
}