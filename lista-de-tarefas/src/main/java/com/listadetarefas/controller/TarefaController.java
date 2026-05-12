package com.listadetarefas.controller;

import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.service.TarefaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
public class TarefaController {

    private final TarefaService tarefaService;

    private static final Logger logger = LoggerFactory.getLogger(TarefaController.class);

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }


    @Operation(summary = "Cria uma nova tarefa", description = "Persiste uma nova tarefa no banco de dados e notifica o usuário de forma assíncrona.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos enviados"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")
    })
    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @RequestBody @Valid TarefaCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long usuarioId) {

        logger.info("Requisicao para criar tarefa '{}' para o usuario com ID {} recebida", request.nome(), usuarioId);

        TarefaResponseDTO response = tarefaService.criarTarefa(request, usuarioId);

        logger.info("Tarefa '{}' criada com sucesso. Tarefa ID: {}", response.nome(), response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Busca uma tarefa por ID", description = "Busca uma tarefa específica pelo seu ID no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarefa encontrada"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Long usuarioId) {

        return ResponseEntity.ok(tarefaService.buscarPorId(id, usuarioId));
    }

    @Operation(summary = "Lista tarefas", description = "Busca tarefas pertencentes ao usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarefa(s) encontrada(s)"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")})
    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal Long usuarioId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Prioridade prioridade) {

        return ResponseEntity.ok(tarefaService.buscarTarefasPorStatusOuPrioridade(usuarioId, status, prioridade));
    }

    @Operation(summary = "Atualiza tarefa", description = "Atualiza tarefa com campos especificados. Campos não enviados ou nulos não serão alterados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarefa atualizada"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "403", description = "A tarefa com ID especificado não pertence ao usuário"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")})
    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid TarefaUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long usuarioId) {

        return ResponseEntity.ok(tarefaService.atualizarTarefa(id, request, usuarioId));
    }

    @Operation(summary = "Deleta tarefa", description = "Deleta tarefa com ID especificado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tarefa deletada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Long usuarioId) {

        tarefaService.deletarTarefa(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
