package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;

public record TarefaUpdateRequestDTO(
        @Schema(description = "Nome da tarefa", example = "Estudar Java")
        String nome,

        @Schema(description = "Se uma tarefa está concluída ou não", example = "Concluída")
        Status status,

        @Schema(description = "Nível de urgência da tarefa", example = "ALTA")
        Prioridade prioridade,

        Long usuarioId
) {}
