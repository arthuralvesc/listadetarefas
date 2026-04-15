package com.listadetarefas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.security.SecurityFilter;
import com.listadetarefas.service.TarefaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TarefaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TarefaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TarefaService tarefaService;

    @MockBean
    private SecurityFilter securityFilter;

    private final Long USUARIO_LOGADO_ID = 1L;
    private TarefaResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USUARIO_LOGADO_ID, null, List.of())
        );

        responseDTO = new TarefaResponseDTO(
                100L,
                "Estudar Spring Boot e Testes",
                Status.NAO_CONCLUIDA,
                Prioridade.ALTA,
                USUARIO_LOGADO_ID
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /tarefas - Criação")
    class CriacaoDeTarefas {

        @Test
        @DisplayName("Deve retornar 201 Created e a tarefa recém-criada")
        void deveCriarTarefa() throws Exception {
            TarefaCreateRequestDTO requestDTO = new TarefaCreateRequestDTO("Estudar Spring Boot e Testes", Prioridade.ALTA);

            when(tarefaService.criarTarefa(any(TarefaCreateRequestDTO.class), eq(USUARIO_LOGADO_ID)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/tarefas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))

                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.nome").value("Estudar Spring Boot e Testes"))
                    .andExpect(jsonPath("$.status").value("NAO_CONCLUIDA"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.usuarioId").value(USUARIO_LOGADO_ID));
        }
    }

    @Nested
    @DisplayName("GET /tarefas - Listagem e Filtros")
    class ListagemDeTarefas {

        @Test
        @DisplayName("Deve retornar 200 OK e a lista filtrada por status e prioridade")
        void deveListarTarefasComFiltros() throws Exception {
            when(tarefaService.buscarTarefasPorStatusOuPrioridade(USUARIO_LOGADO_ID, Status.NAO_CONCLUIDA, Prioridade.ALTA))
                    .thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/tarefas")
                            .param("status", "NAO_CONCLUIDA")
                            .param("prioridade", "ALTA")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nome").value("Estudar Spring Boot e Testes"));
        }
    }

    @Nested
    @DisplayName("GET /tarefas/{id} - Busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar 200 OK e a tarefa solicitada")
        void deveBuscarPorId() throws Exception {
            when(tarefaService.buscarPorId(100L, USUARIO_LOGADO_ID)).thenReturn(responseDTO);

            mockMvc.perform(get("/tarefas/100")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L));
        }
    }

    @Nested
    @DisplayName("PUT /tarefas/{id} - Atualização")
    class AtualizacaoDeTarefas {

        @Test
        @DisplayName("Deve retornar 200 OK e a tarefa atualizada")
        void deveAtualizarTarefa() throws Exception {
            TarefaUpdateRequestDTO requestDTO = new TarefaUpdateRequestDTO(null, Status.CONCLUIDA, null, null);

            TarefaResponseDTO responseAtualizada = new TarefaResponseDTO(
                    100L,
                    "Estudar Spring Boot e Testes",
                    Status.CONCLUIDA,
                    Prioridade.ALTA,
                    USUARIO_LOGADO_ID
            );

            when(tarefaService.atualizarTarefaParcialmente(eq(100L), eq(USUARIO_LOGADO_ID), any(TarefaUpdateRequestDTO.class)))
                    .thenReturn(responseAtualizada);

            mockMvc.perform(put("/tarefas/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
        }
    }

    @Nested
    @DisplayName("DELETE /tarefas/{id} - Exclusão")
    class ExclusaoDeTarefas {

        @Test
        @DisplayName("Deve retornar 204 No Content após deletar")
        void deveDeletarTarefa() throws Exception {
            doNothing().when(tarefaService).deletarTarefa(100L, USUARIO_LOGADO_ID);

            mockMvc.perform(delete("/tarefas/100"))
                    .andExpect(status().isNoContent());

            verify(tarefaService, times(1)).deletarTarefa(100L, USUARIO_LOGADO_ID);
        }
    }
}