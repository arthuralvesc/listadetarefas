package com.listadetarefas.service;

import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.dto.TarefaCursorResponseDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.exception.TarefaNaoEncontradaException;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import com.listadetarefas.repository.TarefaRepository;
import com.listadetarefas.repository.spec.TarefaSpecs;
import com.listadetarefas.util.CursorUtil;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsuarioIntegrationService usuarioIntegrationService;
    private final CursorUtil cursorUtil;

    public TarefaCursorResponseDTO buscarTarefas(Long usuarioId, Status status, Prioridade prioridade, String cursorBase64, int tamanhoPagina) {

        log.info("Buscando tarefas dinâmicas do usuário {}...", usuarioId);

        final ScrollPosition scrollPosition = cursorUtil.resolverScrollPosition(cursorBase64);

        Specification<Tarefa> spec = Specification.where(TarefaSpecs.pertenceAoUsuario(usuarioId))
                .and(TarefaSpecs.comStatus(status))
                .and(TarefaSpecs.comPrioridade(prioridade));

        var window = tarefaRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Direction.DESC, "dataCriacao", "id"))
                .limit(tamanhoPagina)
                .scroll(scrollPosition)
        );

        List<TarefaResponseDTO> tarefas = window.getContent().stream()
                .map(this::converterParaDTO)
                .toList();

        String nextCursor = null;
        if (window.hasNext()) {
            KeysetScrollPosition nextPosition = (KeysetScrollPosition) window.positionAt(window.getContent().size() - 1);
            nextCursor = cursorUtil.codificarCursor(nextPosition.getKeys());
        }

        return new TarefaCursorResponseDTO(tarefas, nextCursor, window.hasNext());
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
