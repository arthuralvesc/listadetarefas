package com.listadetarefas.notificacao.service;

public interface EmailService {
    void enviar(String destinatario, String nomeUsuario, String tipoEvento, String assunto, String corpo);
}
