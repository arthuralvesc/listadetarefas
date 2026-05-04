package com.listadetarefas.usuarios.controller;

import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

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

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(service.listarTodosUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obterUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarUsuarioPorId(id));
    }
}