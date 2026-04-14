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

    // 5. @Nested agrupa testes que compartilham um contexto, criando uma "árvore" no relatório final
    @Nested
    @DisplayName("Cenários de Sucesso")
    class CenariosDeSucesso {

        @Test
        @DisplayName("Deve retornar um token quando email e senha estiverem corretos")
        void deveAutenticarComSucesso() {
            // Dado que (Given) o repositório encontre o usuário e a senha bata
            when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioMock));
            when(passwordEncoder.matches(dto.senha(), usuarioMock.getSenha())).thenReturn(true);
            when(tokenService.gerarToken(usuarioMock)).thenReturn("token_jwt_valido");

            // Quando (When) eu chamar o método autenticar
            String tokenResult = authService.autenticar(dto);

            // Então (Then) eu espero que o token não seja nulo e os métodos tenham sido chamados
            assertEquals("token_jwt_valido", tokenResult);
            verify(tokenService, times(1)).gerarToken(usuarioMock); // Garante que o gerador de token foi acionado
        }
    }

    @Nested
    @DisplayName("Cenários de Falha")
    class CenariosDeFalha {

        @Test
        @DisplayName("Deve lançar exceção quando usuário não for encontrado")
        void deveFalharQuandoUsuarioNaoExiste() {
            // Dado que (Given) o banco retorne vazio
            when(repository.findByEmail(dto.email())).thenReturn(Optional.empty());

            // Quando/Então (When/Then) espero uma IllegalArgumentException
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.autenticar(dto);
            });

            assertEquals("Usuário não encontrado", exception.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString()); // Garante que nem tentou checar a senha
        }

        @Test
        @DisplayName("Deve lançar exceção quando a senha estiver incorreta")
        void deveFalharQuandoSenhaIncorreta() {
            // Dado que (Given) o usuário existe, mas o encoder diz que a senha não bate
            when(repository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioMock));
            when(passwordEncoder.matches(dto.senha(), usuarioMock.getSenha())).thenReturn(false);

            // Quando/Então (When/Then)
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.autenticar(dto);
            });

            assertEquals("Senha incorreta", exception.getMessage());
            verify(tokenService, never()).gerarToken(any()); // Garante que não gerou token por acidente
        }
    }
}