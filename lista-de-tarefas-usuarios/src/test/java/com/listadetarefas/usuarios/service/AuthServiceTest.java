package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.dto.LoginRequestDTO;
import com.listadetarefas.usuarios.model.Usuario;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private LoginRequestDTO dto;
    private Usuario usuarioMock;

    @BeforeEach
    void setup() {
        dto = new LoginRequestDTO("arthur@exemplo.com", "senha123");
        usuarioMock = new Usuario();
        usuarioMock.setEmail("arthur@exemplo.com");
        usuarioMock.setSenha("senha_criptografada_do_banco");
    }

    @Nested
    @DisplayName("Cenários de Sucesso")
    class CenariosDeSucesso {

        @Test
        @DisplayName("Deve retornar um token quando email e senha estiverem corretos")
        void deveAutenticarComSucesso() {
            when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioMock));
            when(passwordEncoder.matches(dto.senha(), usuarioMock.getSenha())).thenReturn(true);
            when(tokenService.gerarToken(usuarioMock)).thenReturn("token_jwt_valido");

            String tokenResult = authService.autenticar(dto);

            assertEquals("token_jwt_valido", tokenResult);
            verify(tokenService, times(1)).gerarToken(usuarioMock);
        }
    }

    @Nested
    @DisplayName("Cenários de Falha")
    class CenariosDeFalha {

        @Test
        @DisplayName("Deve lançar exceção quando usuário não for encontrado")
        void deveFalharQuandoUsuarioNaoExiste() {
            when(repository.findByEmail(dto.email())).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.autenticar(dto);
            });

            assertEquals("Usuário não encontrado", exception.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a senha estiver incorreta")
        void deveFalharQuandoSenhaIncorreta() {
            when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioMock));
            when(passwordEncoder.matches(dto.senha(), usuarioMock.getSenha())).thenReturn(false);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.autenticar(dto);
            });

            assertEquals("Senha incorreta", exception.getMessage());
            verify(tokenService, never()).gerarToken(any());
        }
    }
}