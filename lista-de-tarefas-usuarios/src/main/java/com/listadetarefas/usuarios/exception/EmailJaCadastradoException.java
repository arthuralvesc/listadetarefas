package com.listadetarefas.usuarios.exception;

// Erro para quando o email já existe
public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("O email " + email + " já está em uso no sistema.");
    }
}