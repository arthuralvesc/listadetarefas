package com.listadetarefas.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(
        @Schema(description = "Nome do usuário", example = "Arthur Carvalho")
        @NotBlank String nome,

        @Schema(description = "Email do usuário", example = "arthur@teste.com")
        @Email @NotBlank String email,

        @Schema(description = "Senha do usuário", example = "123123")
        @NotBlank String senha
) {}

