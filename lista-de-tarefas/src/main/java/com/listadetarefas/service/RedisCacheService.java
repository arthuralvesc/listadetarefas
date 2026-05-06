package com.listadetarefas.service;

import com.listadetarefas.dto.UsuarioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);
    private final CacheManager cacheManager;
    private static final String CACHE_NOME = "usuarios";

    public RedisCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void salvarUsuario(UsuarioDTO usuario) {
        if (usuario == null || usuario.getId() == null) return;

        Cache cache = cacheManager.getCache(CACHE_NOME);
        if (cache != null) {
            cache.put(usuario.getId(), usuario);
            logger.debug("Usuário {} salvo no cache de segurança do Redis.", usuario.getId());
        }
    }

    public UsuarioDTO buscarUsuario(Long id) {
        Cache cache = cacheManager.getCache(CACHE_NOME);
        if (cache != null) {
            return cache.get(id, UsuarioDTO.class);
        }
        return null;
    }
}