package com.listadetarefas.controller;

import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.service.TarefaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @RequestBody @Valid TarefaCreateRequestDTO request,
            @AuthenticationPrincipal Long usuarioId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tarefaService.criarTarefa(request, usuarioId));
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

        return ResponseEntity.ok(tarefaService.atualizarTarefaParcialmente(id, usuarioId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Long usuarioId) {

        tarefaService.deletarTarefa(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
