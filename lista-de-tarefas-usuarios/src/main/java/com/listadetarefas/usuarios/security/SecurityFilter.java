package com.listadetarefas.usuarios.security;

import com.listadetarefas.usuarios.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public SecurityFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var token = this.recuperarToken(request);

        // Guard Clause 1: Se não tem token, passa direto
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var email = tokenService.validarToken(token);

        // Guard Clause 2: Se o token for falso/expirado, passa direto
        if (email.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Caminho Feliz (Happy Path): Sem aninhamento.
        // Token existe e passou na validação. O usuário está logado.
        var authentication = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Segue o fluxo informando ao Spring que este usuário tem acesso
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;

        // O padrão da web é enviar o token assim: "Bearer eyJhbGc..."
        // Então nós cortamos a palavra Bearer para sobrar só o JWT puro
        return authHeader.replace("Bearer ", "");
    }
}