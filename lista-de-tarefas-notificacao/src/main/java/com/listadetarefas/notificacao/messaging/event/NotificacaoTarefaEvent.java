package com.listadetarefas.notificacao.messaging.event;

public record NotificacaoTarefaEvent(
        String emailDestinatario,
        String nomeUsuario,
        String tipoEvento,
        String tituloTarefa
) {}