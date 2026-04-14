package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date dataCriacao = new Date();
        Date dataExpiracao = new Date(dataCriacao.getTime() + (1000 * 60 * 60 * 2));

        return Jwts.builder()
                .issuer("lista-de-tarefas-api")
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .issuedAt(dataCriacao)
                .expiration(dataExpiracao)
                .signWith(key)
                .compact();
    }

    public String validarToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

        } catch (JwtException exception) {
            return "";
        }
    }
}