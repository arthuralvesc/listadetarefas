package com.listadetarefas.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioResponseDTO(

        @Schema(description = "ID do usuário", example = "1L")
        Long id,

        @Schema(description = "Nome do usuário", example = "Arthur Carvalho")
        String nome,

        @Schema(description = "Email do usuário", example = "arthur@teste.com")
        String email
) {}
