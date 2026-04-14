package com.listadetarefas.dto;

import com.listadetarefas.model.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TarefaCreateRequestDTO(
        @NotBlank(message = "O nome da tarefa é obrigatório.") String nome,
        @NotNull(message = "A prioridade não pode ser nula.") Prioridade prioridade) {}
