package com.listadetarefas.usuarios.controller;

import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrarUsuario(@RequestBody @Valid UsuarioRequestDTO dto) {
        return ResponseEntity.ok(service.criarUsuario(dto));
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