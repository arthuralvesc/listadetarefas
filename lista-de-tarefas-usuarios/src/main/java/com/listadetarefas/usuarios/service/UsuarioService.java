package com.listadetarefas.usuarios.service;

import com.listadetarefas.usuarios.config.RabbitMQConfig;
import com.listadetarefas.usuarios.event.UsuarioCriadoEvent;
import com.listadetarefas.usuarios.exception.EmailJaCadastradoException;
import com.listadetarefas.usuarios.exception.UsuarioNaoEncontradoException;
import com.listadetarefas.usuarios.model.Usuario;
import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.dto.UsuarioResponseDTO;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());

        String senhaComHash = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaComHash);

        usuario = repository.save(usuario);

        UsuarioCriadoEvent evento = new UsuarioCriadoEvent(usuario.getId(), usuario.getNome(), usuario.getEmail());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "", evento);

        return converterParaDTO(usuario);
    }

    public UsuarioResponseDTO buscarUsuarioPorId(Long usuarioId) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
        return converterParaDTO(usuario);
    }

    public List<UsuarioResponseDTO> listarTodosUsuarios() {
        return repository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail()
        );
    }
}