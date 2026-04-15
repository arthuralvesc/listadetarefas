package com.listadetarefas.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private final String SECRET = "MinhaChaveSuperSecretaParaTestesDeJwtComPeloMenos32Caracteres";

    @BeforeEach
    void setup() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
    }

    @Test
    @DisplayName("Deve extrair o ID do usuário de um token válido")
    void extrairIdComSucesso() {
        String tokenValido = Jwts.builder()
                .claim("id", 55L)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Long id = tokenService.extrairIdDoUsuario(tokenValido);
        assertEquals(55L, id);
    }

    @Test
    @DisplayName("Deve retornar null quando o token for inválido ou adulterado")
    void extrairIdFalha() {
        Long id = tokenService.extrairIdDoUsuario("token.totalmente.falso");
        assertNull(id);
    }
}