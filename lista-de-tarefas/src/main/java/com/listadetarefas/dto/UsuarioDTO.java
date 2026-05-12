package com.listadetarefas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {

    @Schema(description = "ID do usuário", example = "1L")
    private Long id;

    @Schema(description = "Nome do usuário", example = "Arthur")
    private String nome;

    @Schema(description = "Email do usuário", example = "arthur@teste.com")
    private String email;
}