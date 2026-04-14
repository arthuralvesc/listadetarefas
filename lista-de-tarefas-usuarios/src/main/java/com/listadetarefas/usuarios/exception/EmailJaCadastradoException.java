package com.listadetarefas.usuarios.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("O email " + email + " já está em uso no sistema.");
    }
}