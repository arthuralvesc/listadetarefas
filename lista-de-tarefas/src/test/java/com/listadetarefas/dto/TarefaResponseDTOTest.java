package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TarefaResponseDTOTest {

    @Test
    @DisplayName("Deve converter a entidade Tarefa para TarefaResponseDTO corretamente")
    void deveConverterEntityParaDTO() {
        Tarefa tarefaEntity = new Tarefa(
                50L,
                "Dominar Testes Unitários com JaCoCo",
                Prioridade.ALTA,
                Status.CONCLUIDA,
                77L,
                LocalDateTime.now(),
                null
        );

        TarefaResponseDTO dto = new TarefaResponseDTO(tarefaEntity);

        assertEquals(50L, dto.id());
        assertEquals("Dominar Testes Unitários com JaCoCo", dto.nome());
        assertEquals(Prioridade.ALTA, dto.prioridade());
        assertEquals(Status.CONCLUIDA, dto.status());
        assertEquals(77L, dto.usuarioId());
    }
}