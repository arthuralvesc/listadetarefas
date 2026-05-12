package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TarefaCreateRequestDTO(
        @Schema(description = "Título principal da tarefa", example = "Estruturar backend do projeto")
        @NotBlank(message = "O nome da tarefa é obrigatório.") String nome,

        @Schema(description = "Nível de urgência da tarefa", example = "ALTA")
        @NotNull(message = "A prioridade não pode ser nula.") Prioridade prioridade) {}
