package com.listadetarefas.usuarios.event;

public record UsuarioCriadoEvent(
        Long id,
        String nome,
        String email
) {}
