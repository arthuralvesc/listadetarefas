package com.listadetarefas.service;


import com.listadetarefas.client.UsuarioClient;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.service.RedisCacheService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UsuarioIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioIntegrationService.class);

    private final UsuarioClient usuarioClient;
    private final RedisCacheService redisCache;

    public UsuarioIntegrationService(UsuarioClient usuarioClient, RedisCacheService redisCache) {
        this.usuarioClient = usuarioClient;
        this.redisCache = redisCache;
    }

    @CircuitBreaker(name = "lista-de-tarefas-usuarios", fallbackMethod = "fallbackBuscarUsuario")
    public UsuarioDTO buscarDetalhesUsuario(Long usuarioId) {
        logger.info("Tentando buscar detalhes do usuarioId: {} via Feign Client", usuarioId);

        UsuarioDTO usuario = usuarioClient.buscarUsuarioPorId(usuarioId);

        redisCache.salvarUsuario(usuario);

        return usuario;
    }

    public UsuarioDTO fallbackBuscarUsuario(Long usuarioId, Exception e) {
        logger.warn("Falha de comunicação via Feign para o usuarioId: {}. Motivo: {}. Acionando Redis...", usuarioId, e.getMessage());

        UsuarioDTO usuarioEmCache = redisCache.buscarUsuario(usuarioId);

        if (usuarioEmCache != null) {
            logger.info("Plano B ativado: Usuário recuperado com sucesso do Redis.");
            return usuarioEmCache;
        }

        logger.error("Cache inexistente no Redis para o usuarioId: {}. Retornando perfil anônimo.", usuarioId);
        return new UsuarioDTO(
                usuarioId,
                "Usuário Indisponível",
                "email.padrao@listadetarefas.com"
        );
    }
}