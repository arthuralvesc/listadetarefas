package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;

public record TarefaResponseDTO(
        Long id,
        String nome,
        Status status,
        Prioridade prioridade,
        Long usuarioId
) {
    public TarefaResponseDTO(Tarefa tarefa){
        this(tarefa.getId(), tarefa.getNome(), tarefa.getStatus(), tarefa.getPrioridade(), tarefa.getUsuarioId());
    }

}
