package com.listadetarefas.service;

import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.exception.TarefaNaoEncontradaException;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import com.listadetarefas.repository.TarefaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TarefaService {

    private final TarefaRepository tarefaRepository;

    public TarefaService(TarefaRepository tarefaRepository){
        this.tarefaRepository = tarefaRepository;
    }

    public List<TarefaResponseDTO> buscarTarefasPorStatusOuPrioridade(Long usuarioId, Status status, Prioridade prioridade) {

        if (status != null && prioridade != null) {
            return tarefaRepository.findByUsuarioIdAndStatusAndPrioridade(usuarioId, status, prioridade)
                    .stream().map(this::converterParaDTO).toList();
        }

        if (status != null) {
            return tarefaRepository.findByUsuarioIdAndStatus(usuarioId, status)
                    .stream().map(this::converterParaDTO).toList();
        }

        if (prioridade != null) {
            return tarefaRepository.findByUsuarioIdAndPrioridade(usuarioId, prioridade)
                    .stream().map(this::converterParaDTO).toList();
        }

        return tarefaRepository.findByUsuarioId(usuarioId)
                .stream().map(this::converterParaDTO).toList();
    }

    public TarefaResponseDTO buscarPorId(Long id, Long usuarioId) {
        Tarefa tarefa = buscarTarefaPorIdEUsuario(id, usuarioId);
        return converterParaDTO(tarefa);
    }

    @Transactional
    public TarefaResponseDTO criarTarefa(TarefaCreateRequestDTO request, Long usuarioId) {

        Tarefa novaTarefa = new Tarefa();
        novaTarefa.setNome(request.nome());
        novaTarefa.setStatus(Status.NAO_CONCLUIDA);
        novaTarefa.setPrioridade(request.prioridade());

        novaTarefa.setUsuarioId(usuarioId);

        novaTarefa = tarefaRepository.save(novaTarefa);
        return converterParaDTO(novaTarefa);
    }

    @Transactional
    public TarefaResponseDTO atualizarTarefaParcialmente(Long id, Long usuarioId, TarefaUpdateRequestDTO request) {
        Tarefa tarefa = buscarTarefaPorIdEUsuario(id, usuarioId);

        if (request.nome() != null && !request.nome().trim().isEmpty()) {
            tarefa.setNome(request.nome());
        }

        if (request.status() != null) {
            tarefa.setStatus(request.status());
        }

        if (request.prioridade() != null) {
            tarefa.setPrioridade(request.prioridade());
        }

        tarefa = tarefaRepository.save(tarefa);
        return converterParaDTO(tarefa);
    }

    @Transactional
    public void deletarTarefa(Long id, Long usuarioId) {
        Tarefa tarefa = buscarTarefaPorIdEUsuario(id, usuarioId);
        tarefaRepository.delete(tarefa);
    }

    private Tarefa buscarTarefaPorIdEUsuario(Long id, Long usuarioId) {
        return tarefaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new TarefaNaoEncontradaException(id, usuarioId));
    }

    private TarefaResponseDTO converterParaDTO(Tarefa tarefa) {
        return new TarefaResponseDTO(
                tarefa.getId(),
                tarefa.getNome(),
                tarefa.getStatus(),
                tarefa.getPrioridade(),
                tarefa.getUsuarioId()
        );
    }
}