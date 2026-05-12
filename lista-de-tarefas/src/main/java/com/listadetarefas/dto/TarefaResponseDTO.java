package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import io.swagger.v3.oas.annotations.media.Schema;

public record TarefaResponseDTO(

        @Schema(description = "ID primário da tarefa", example = "1")
        Long id,

        @Schema(description = "Nome da tarefa", example = "Estudar Java")
        String nome,

        @Schema(description = "Se uma tarefa está concluída ou não", example = "Concluída")
        Status status,

        @Schema(description = "Nível de urgência da tarefa", example = "ALTA")
        Prioridade prioridade,

        Long usuarioId
) {
    public TarefaResponseDTO(Tarefa tarefa){
        this(tarefa.getId(), tarefa.getNome(), tarefa.getStatus(), tarefa.getPrioridade(), tarefa.getUsuarioId());
    }

}
