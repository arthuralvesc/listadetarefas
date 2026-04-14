package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;

public record TarefaUpdateRequestDTO(
        String nome,
        Status status,
        Prioridade prioridade,
        Long usuarioId
) {}
