package com.listadetarefas.controller;

import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.service.TarefaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/tarefas")
@Validated
public class TarefaController {

    private final TarefaService tarefaService;

    private static final Logger logger = LoggerFactory.getLogger(TarefaController.class);

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @RequestBody @Valid TarefaCreateRequestDTO request,
            @AuthenticationPrincipal Long usuarioId) {

        logger.info("Requisicao para criar tarefa '{}' para o usuario com ID {} recebida", request.nome(), usuarioId);

        TarefaResponseDTO response = tarefaService.criarTarefa(request, usuarioId);

        logger.info("Tarefa '{}' criada com sucesso. Tarefa ID: {}", response.nome(), response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Long usuarioId) {

        return ResponseEntity.ok(tarefaService.buscarPorId(id, usuarioId));
    }

    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listar(
            @AuthenticationPrincipal Long usuarioId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Prioridade prioridade) {

        return ResponseEntity.ok(tarefaService.buscarTarefasPorStatusOuPrioridade(usuarioId, status, prioridade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid TarefaUpdateRequestDTO request,
            @AuthenticationPrincipal Long usuarioId) {

        return ResponseEntity.ok(tarefaService.atualizarTarefa(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Long usuarioId) {

        tarefaService.deletarTarefa(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
