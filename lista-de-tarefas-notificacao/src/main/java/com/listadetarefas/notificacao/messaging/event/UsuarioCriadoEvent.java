package com.listadetarefas.notificacao.messaging.event;

public record UsuarioCriadoEvent(
        Long id,
        String nome,
        String email
) {}
