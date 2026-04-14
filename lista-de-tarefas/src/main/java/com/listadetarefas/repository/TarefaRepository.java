package com.listadetarefas.repository;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByUsuarioId(Long usuarioId);

    Optional<Tarefa> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Tarefa> findByUsuarioIdAndStatus(Long usuarioId, Status status);

    List<Tarefa> findByUsuarioIdAndPrioridade(Long usuarioId, Prioridade prioridade);

    List<Tarefa> findByUsuarioIdAndStatusAndPrioridade(Long usuarioId, Status status, Prioridade prioridade);
}