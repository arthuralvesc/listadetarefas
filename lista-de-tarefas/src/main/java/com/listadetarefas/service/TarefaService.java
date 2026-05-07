package com.listadetarefas.service;

import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.exception.TarefaNaoEncontradaException;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import com.listadetarefas.repository.TarefaRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsuarioIntegrationService usuarioIntegrationService;

    @Cacheable(value = "tarefas", key = "#usuarioId", condition = "#status == null && #prioridade == null")
    public List<TarefaResponseDTO> buscarTarefasPorStatusOuPrioridade(Long usuarioId, Status status, Prioridade prioridade) {


        if (status != null && prioridade != null) {
            log.info("Buscando tarefas do usuário {} no banco de dados...", usuarioId);
            return tarefaRepository.findByUsuarioIdAndStatusAndPrioridade(usuarioId, status, prioridade)
                    .stream().map(this::converterParaDTO).collect(Collectors.toList());
        }

        if (status != null) {
            log.info("Buscando tarefas do usuário {} no banco de dados...", usuarioId);
            return tarefaRepository.findByUsuarioIdAndStatus(usuarioId, status)
                    .stream().map(this::converterParaDTO).collect(Collectors.toList());
        }

        if (prioridade != null) {
            log.info("Buscando tarefas do usuário {} no banco de dados...", usuarioId);
            return tarefaRepository.findByUsuarioIdAndPrioridade(usuarioId, prioridade)
                    .stream().map(this::converterParaDTO).collect(Collectors.toList());
        }

        return tarefaRepository.findByUsuarioId(usuarioId)
                .stream().map(this::converterParaDTO).collect(Collectors.toList());
    }

    public TarefaResponseDTO buscarPorId(Long id, Long usuarioId) {
        Tarefa tarefa = buscarTarefaPorIdEUsuario(id, usuarioId);
        return converterParaDTO(tarefa);
    }

    @CacheEvict(value = "tarefas", key = "#usuarioId")
    public TarefaResponseDTO criarTarefa(TarefaCreateRequestDTO request, Long usuarioId) {
        Tarefa novaTarefa = persistirNovaTarefa(request, usuarioId);

        orquestrarNotificacao(usuarioId, "TAREFA_CRIADA", novaTarefa.getNome());

        return converterParaDTO(novaTarefa);
    }

    @Transactional
    @CacheEvict(value = "tarefas", key = "#usuarioId")
    public TarefaResponseDTO atualizarTarefa(Long id, TarefaUpdateRequestDTO request, Long usuarioId) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new TarefaNaoEncontradaException(id, usuarioId));

        if (!tarefa.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Acesso negado a esta tarefa");
        }

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

        orquestrarNotificacao(usuarioId, "TAREFA_ATUALIZADA", tarefa.getNome());

        return converterParaDTO(tarefa);
    }

    @Transactional
    @CacheEvict(value = "tarefas", key = "#usuarioId")
    public void deletarTarefa(Long id, Long usuarioId) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new TarefaNaoEncontradaException(id, usuarioId));

        if (!tarefa.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Acesso negado a esta tarefa");
        }

        String nomeTarefaDeletada = tarefa.getNome();

        tarefaRepository.delete(tarefa);

        orquestrarNotificacao(usuarioId, "TAREFA_DELETADA", nomeTarefaDeletada);
    }

    private Tarefa persistirNovaTarefa(TarefaCreateRequestDTO request, Long usuarioId) {
        Tarefa tarefa = new Tarefa();
        tarefa.setNome(request.nome());
        tarefa.setStatus(Status.NAO_CONCLUIDA);
        tarefa.setPrioridade(request.prioridade());
        tarefa.setUsuarioId(usuarioId);
        return tarefaRepository.save(tarefa);
    }

    private void orquestrarNotificacao(Long usuarioId, String tipoEvento, String tituloTarefa) {
        try {
            UsuarioDTO usuario = usuarioIntegrationService.buscarDetalhesUsuario(usuarioId);

            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent(
                    usuario.getEmail(),
                    usuario.getNome(),
                    tipoEvento,
                    tituloTarefa
            );

            enviarMensagemParaFila(evento);

        } catch (Exception e) {
            log.error("Erro ao orquestrar a notificação '{}' para a tarefa '{}'. O e-mail não será enviado.", tipoEvento, tituloTarefa, e);
        }
    }

    private void enviarMensagemParaFila(NotificacaoTarefaEvent evento) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.FILA_NOTIFICACOES, evento);
        log.info("Evento '{}' da tarefa '{}' postado na fila com sucesso.", evento.tipoEvento(), evento.tituloTarefa());
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
