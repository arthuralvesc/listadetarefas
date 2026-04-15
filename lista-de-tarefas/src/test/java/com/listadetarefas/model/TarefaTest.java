package com.listadetarefas.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TarefaTest {

    private Tarefa tarefa;

    @BeforeEach
    void setup() {
        tarefa = new Tarefa();
        tarefa.setNome("Comprar café");
        tarefa.setPrioridade(Prioridade.ALTA);
        tarefa.setStatus(Status.NAO_CONCLUIDA);
        tarefa.setUsuarioId(1L);
    }

    @Nested
    @DisplayName("Testes de Callbacks do JPA (Ciclo de Vida)")
    class CallbacksJPA {

        @Test
        @DisplayName("Deve preencher a data de criação ao executar o callback @PrePersist")
        void devePreencherDataCriacaoAoCriar() {
            assertNull(tarefa.getDataCriacao());

            tarefa.aoCriar();

            assertNotNull(tarefa.getDataCriacao());

            assertTrue(tarefa.getDataCriacao().isAfter(LocalDateTime.now().minusSeconds(1)));
            assertTrue(tarefa.getDataCriacao().isBefore(LocalDateTime.now().plusSeconds(1)));
        }

        @Test
        @DisplayName("Deve preencher a data de atualização ao executar o callback @PreUpdate")
        void devePreencherDataAtualizacaoAoAtualizar() {
            assertNull(tarefa.getDataAtualizacao());

            tarefa.aoAtualizar();

            assertNotNull(tarefa.getDataAtualizacao());

            assertTrue(tarefa.getDataAtualizacao().isAfter(LocalDateTime.now().minusSeconds(1)));
            assertTrue(tarefa.getDataAtualizacao().isBefore(LocalDateTime.now().plusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("Construtores e Lombok")
    class Construtores {

        @Test
        @DisplayName("Deve popular os dados corretamente utilizando o AllArgsConstructor")
        void deveCriarTarefaComTodosOsArgumentos() {
            LocalDateTime agora = LocalDateTime.now();

            Tarefa tarefaCompleta = new Tarefa(
                    100L,
                    "Revisar código",
                    Prioridade.MEDIA,
                    Status.CONCLUIDA,
                    2L,
                    agora,
                    agora
            );

            assertEquals(100L, tarefaCompleta.getId());
            assertEquals("Revisar código", tarefaCompleta.getNome());
            assertEquals(Prioridade.MEDIA, tarefaCompleta.getPrioridade());
            assertEquals(Status.CONCLUIDA, tarefaCompleta.getStatus());
            assertEquals(2L, tarefaCompleta.getUsuarioId());
            assertEquals(agora, tarefaCompleta.getDataCriacao());
            assertEquals(agora, tarefaCompleta.getDataAtualizacao());
        }
    }
}