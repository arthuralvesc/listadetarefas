package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.dto.LoginRequestDTO;
import com.listadetarefas.usuarios.model.Usuario;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public String autenticar(LoginRequestDTO loginRequestDTO) {
        Usuario usuario = repository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(loginRequestDTO.senha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Senha incorreta");
        }

        return tokenService.gerarToken(usuario);
    }
}