package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private Usuario usuarioMock;

    private final String SECRET_MOCK = "MinhaChaveSuperSecretaParaTestesDeJwtComPeloMenos32Caracteres";

    @BeforeEach
    void setup() {
        tokenService = new TokenService();

        ReflectionTestUtils.setField(tokenService, "secret", SECRET_MOCK);

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setEmail("arthur@exemplo.com");
    }

    @Nested
    @DisplayName("Geração de Token")
    class GeracaoDeToken {

        @Test
        @DisplayName("Deve gerar um token JWT válido com 3 partes (Header, Payload, Signature)")
        void deveGerarToken() {
            String token = tokenService.gerarToken(usuarioMock);

            assertNotNull(token);
            assertFalse(token.isBlank());

            String[] partesDoToken = token.split("\\.");
            assertEquals(3, partesDoToken.length);
        }
    }

    @Nested
    @DisplayName("Validação de Token")
    class ValidacaoDeToken {

        @Test
        @DisplayName("Deve retornar o email (subject) quando o token for válido")
        void deveValidarTokenComSucesso() {
            String token = tokenService.gerarToken(usuarioMock);

            String subject = tokenService.validarToken(token);

            assertEquals(usuarioMock.getEmail(), subject);
        }

        @Test
        @DisplayName("Deve retornar string vazia quando o token for inventado/falso")
        void deveRetornarVazioParaTokenInvalido() {
            String subject = tokenService.validarToken("token.totalmente.invalido");

            assertEquals("", subject);
        }

        @Test
        @DisplayName("Deve retornar string vazia quando o token estiver expirado")
        void deveRetornarVazioParaTokenExpirado() {
            String tokenExpirado = Jwts.builder()
                    .subject(usuarioMock.getEmail())
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(Keys.hmacShaKeyFor(SECRET_MOCK.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            String subject = tokenService.validarToken(tokenExpirado);

            assertEquals("", subject);
        }
    }
}