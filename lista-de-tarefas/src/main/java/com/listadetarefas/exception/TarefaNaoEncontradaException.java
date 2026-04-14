package com.listadetarefas.exception;

public class TarefaNaoEncontradaException extends RuntimeException {

    public TarefaNaoEncontradaException(Long tarefaId, Long usuarioId) {
        super("A tarefa com ID " + tarefaId + " não foi encontrada para o usuário " + usuarioId + ".");
    }
}