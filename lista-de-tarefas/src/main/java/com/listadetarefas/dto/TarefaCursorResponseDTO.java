package com.listadetarefas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarefaCursorResponseDTO(
        List<TarefaResponseDTO> data,
        String nextCursor,
        boolean hasNext
) {}