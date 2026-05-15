package com.listadetarefas.repository.spec;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import org.springframework.data.jpa.domain.Specification;

public class TarefaSpecs {

    private TarefaSpecs() {
        throw new UnsupportedOperationException("Esta é uma classe utilitária e não pode ser instanciada");
    }

    public static Specification<Tarefa> pertenceAoUsuario(Long usuarioId) {
        return (root, query, builder) -> builder.equal(root.get("usuarioId"), usuarioId);
    }

    public static Specification<Tarefa> comStatus(Status status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Tarefa> comPrioridade(Prioridade prioridade) {
        return (root, query, builder) -> prioridade == null ? null : builder.equal(root.get("prioridade"), prioridade);
    }
}