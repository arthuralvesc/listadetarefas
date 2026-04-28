package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.exception.UsuarioNaoEncontradoException;
import com.listadetarefas.usuarios.model.Usuario;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UsuarioService usuarioService;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    private Usuario usuarioMock;
    private UsuarioRequestDTO requestDTO;

    @BeforeEach
    void setup() {
        requestDTO = new UsuarioRequestDTO("Arthur", "arthur@exemplo.com", "senha123");

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("Arthur");
        usuarioMock.setEmail("arthur@exemplo.com");
        usuarioMock.setSenha("hash_da_senha_aqui");
    }

    @Nested
    @DisplayName("Criação de Usuário")
    class CriacaoDeUsuario {

        @Test
        @DisplayName("Deve criptografar a senha, salvar e retornar o DTO")
        void deveCriarUsuarioComSucesso() {
            when(passwordEncoder.encode(requestDTO.senha())).thenReturn("hash_da_senha_aqui");
            when(repository.save(any(Usuario.class))).thenReturn(usuarioMock);

            UsuarioResponseDTO response = usuarioService.criarUsuario(requestDTO);

            assertNotNull(response);
            assertEquals(usuarioMock.getId(), response.id());
            assertEquals(requestDTO.nome(), response.nome());

            verify(repository).save(usuarioCaptor.capture());

            Usuario usuarioSalvo = usuarioCaptor.getValue();

            assertEquals("hash_da_senha_aqui", usuarioSalvo.getSenha());
            assertEquals("Arthur", usuarioSalvo.getNome());
        }
    }

    @Nested
    @DisplayName("Busca de Usuários")
    class BuscaDeUsuarios {

        @Test
        @DisplayName("Deve retornar o DTO quando o ID existir")
        void deveBuscarPorIdComSucesso() {
            when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));

            UsuarioResponseDTO response = usuarioService.buscarUsuarioPorId(1L);

            assertNotNull(response);
            assertEquals(usuarioMock.getId(), response.id());
            assertEquals(usuarioMock.getNome(), response.nome());
        }

        @Test
        @DisplayName("Deve lançar UsuarioNaoEncontradoException quando o ID não existir")
        void deveFalharQuandoIdNaoExiste() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class, () -> {
                usuarioService.buscarUsuarioPorId(99L);
            });
        }
    }

    @Nested
    @DisplayName("Listagem de Usuários")
    class ListagemDeUsuarios {

        @Test
        @DisplayName("Deve retornar uma lista de DTOs")
        void deveListarTodos() {
            Usuario usuario2 = new Usuario();
            usuario2.setId(2L);
            usuario2.setNome("João");
            usuario2.setEmail("joao@exemplo.com");

            when(repository.findAll()).thenReturn(List.of(usuarioMock, usuario2));

            List<UsuarioResponseDTO> lista = usuarioService.listarTodosUsuarios();

            assertNotNull(lista);
            assertEquals(2, lista.size());
            assertEquals("Arthur", lista.get(0).nome());
            assertEquals("João", lista.get(1).nome());
        }

        @Test
        @DisplayName("Deve retornar lista vazia se não houver usuários no banco")
        void deveRetornarListaVazia() {
            when(repository.findAll()).thenReturn(List.of());

            List<UsuarioResponseDTO> lista = usuarioService.listarTodosUsuarios();

            assertNotNull(lista);
            assertTrue(lista.isEmpty());
        }
    }
}