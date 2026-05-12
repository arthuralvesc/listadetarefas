package com.listadetarefas.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

public record LoginRequestDTO(
        @Schema(description = "Email do usuário", example = "arthur@teste.com")
        @Email(message = "O email deve ser válido.")
        String email,

        @Schema(description = "Senha do usuário", example = "123123")
        @Min(value = 6, message = "A senha deve conter pelo menos 6 caracteres.")
        String senha
) {}