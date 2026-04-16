package com.listadetarefas.usuarios.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.usuarios.ListaDeTarefasUsuariosApplication;
import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.model.Usuario;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = ListaDeTarefasUsuariosApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UsuarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Integração (H2): Deve criar um usuário com sucesso e salvar no banco real")
    void deveCadastrarUsuarioComSucesso() throws Exception {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Arthur Carvalho",
                "arthur@email.com",
                "SenhaForte123!"
        );

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Arthur Carvalho"))
                .andExpect(jsonPath("$.email").value("arthur@email.com"));

        assertEquals(1, usuarioRepository.count(), "Deveria ter 1 usuário salvo no H2");

        var usuarioSalvo = usuarioRepository.findAll().getFirst();
        assertEquals("Arthur Carvalho", usuarioSalvo.getNome());
        assertEquals("arthur@email.com", usuarioSalvo.getEmail());

    }
}