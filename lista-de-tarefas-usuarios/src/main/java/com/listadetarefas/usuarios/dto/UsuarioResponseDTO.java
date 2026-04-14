package com.listadetarefas.usuarios.dto;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email
) {}
