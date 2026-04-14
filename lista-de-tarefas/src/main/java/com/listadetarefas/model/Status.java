package com.listadetarefas.model;

import lombok.Getter;

@Getter
public enum Status {
    CONCLUIDA("Concluída"),
    NAO_CONCLUIDA("Não Concluída");

    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }
}