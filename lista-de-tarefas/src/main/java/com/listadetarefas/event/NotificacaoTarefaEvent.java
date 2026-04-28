package com.listadetarefas.event;

public record NotificacaoTarefaEvent(
        String emailDestinatario,
        String nomeUsuario,
        String tipoEvento,
        String tituloTarefa
) {}
