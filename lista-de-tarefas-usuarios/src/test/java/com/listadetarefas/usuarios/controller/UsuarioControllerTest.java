package com.listadetarefas.usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.security.SecurityFilter;
import com.listadetarefas.usuarios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private SecurityFilter securityFilter;

    private UsuarioRequestDTO requestDTO;
    private UsuarioResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new UsuarioRequestDTO("Arthur", "arthur@exemplo.com", "senha123");
        responseDTO = new UsuarioResponseDTO(1L, "Arthur", "arthur@exemplo.com");
    }

    @Nested
    @DisplayName("POST /usuarios - Cadastro")
    class CadastroDeUsuarios {

        @Test
        @DisplayName("Deve retornar 200 OK e o DTO salvo quando os dados forem válidos")
        void deveCadastrarUsuarioComSucesso() throws Exception {
            when(usuarioService.criarUsuario(any(UsuarioRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.nome").value("Arthur"))
                    .andExpect(jsonPath("$.email").value("arthur@exemplo.com"));
        }
    }

    @Nested
    @DisplayName("GET /usuarios - Listagem")
    class ListagemDeUsuarios {

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários")
        void deveListarTodosUsuarios() throws Exception {
            UsuarioResponseDTO usuario2 = new UsuarioResponseDTO(2L, "João", "joao@exemplo.com");

            when(usuarioService.listarTodosUsuarios()).thenReturn(List.of(responseDTO, usuario2));

            mockMvc.perform(get("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nome").value("Arthur"))
                    .andExpect(jsonPath("$[1].nome").value("João"));
        }

        @Test
        @DisplayName("Deve retornar 200 OK e uma lista vazia quando não houver usuários")
        void deveRetornarListaVazia() throws Exception {
            when(usuarioService.listarTodosUsuarios()).thenReturn(List.of());

            mockMvc.perform(get("/usuarios")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /usuarios/{id} - Busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar 200 OK e o usuário correspondente ao ID")
        void deveRetornarUsuarioPorId() throws Exception {
            when(usuarioService.buscarUsuarioPorId(1L)).thenReturn(responseDTO);

            mockMvc.perform(get("/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.nome").value("Arthur"));
        }
    }
}