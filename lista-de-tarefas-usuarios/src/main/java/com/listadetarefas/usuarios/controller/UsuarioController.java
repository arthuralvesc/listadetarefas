package com.listadetarefas.usuarios.controller;

import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService service;

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Cria um novo usuário", description = "Persiste um novo  usuário no banco de dados e envia uma notificação de boas vindas de forma assíncrona.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos enviados"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UsuarioResponseDTO> cadastrarUsuario(@RequestBody @Valid UsuarioRequestDTO dto) {
        logger.info("Iniciando requisicao para criar usuario com email: {}", dto.email());

        try {
            UsuarioResponseDTO response = service.criarUsuario(dto);
            logger.info("Usuario criado com sucesso. ID: {}, Email: {}", response.id(), response.email());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Falha ao criar usuario com email: {}. Causa: {}", dto.email(), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista usuários", description = "Busca usuários do banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário(s) encontrado(s)."),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado no sistema"),
            @ApiResponse(responseCode = "404", description = "Nenhum usuário encontrado"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")
    })
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(service.listarTodosUsuarios());
    }

    @Operation(summary = "Busca usuário pelo ID", description = "Busca usuário específico no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos enviados"),
            @ApiResponse(responseCode = "500", description = "Falha interna no servidor ou timeout")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obterUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarUsuarioPorId(id));
    }
}