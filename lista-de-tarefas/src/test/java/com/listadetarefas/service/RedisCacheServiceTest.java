package com.listadetarefas.service;

import com.listadetarefas.dto.UsuarioDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private RedisCacheService redisCacheService;

    private final Long USUARIO_ID = 1L;
    private final String CACHE_NOME = "usuarios";

    @Test
    @DisplayName("salvarUsuario: Deve retornar silenciosamente se o objeto usuario for nulo")
    void salvarUsuario_RetornaSeUsuarioNulo() {
        redisCacheService.salvarUsuario(null);

        // Verifica que o CacheManager nunca foi acionado
        verify(cacheManager, never()).getCache(anyString());
    }

    @Test
    @DisplayName("salvarUsuario: Deve retornar silenciosamente se o ID do usuario for nulo")
    void salvarUsuario_RetornaSeIdNulo() {
        UsuarioDTO usuarioSemId = new UsuarioDTO(null, "Arthur Carvalho", "arthur@teste.com");

        redisCacheService.salvarUsuario(usuarioSemId);

        verify(cacheManager, never()).getCache(anyString());
    }

    @Test
    @DisplayName("salvarUsuario: Não deve estourar NullPointerException se o cache não existir no Spring")
    void salvarUsuario_IgnoraSeCacheNulo() {
        UsuarioDTO usuarioValido = new UsuarioDTO(USUARIO_ID, "Arthur Carvalho", "arthur@teste.com");
        when(cacheManager.getCache(CACHE_NOME)).thenReturn(null);

        redisCacheService.salvarUsuario(usuarioValido);

        verify(cacheManager, times(1)).getCache(CACHE_NOME);
        // O cache.put() não pode ser chamado, evitamos o NullPointerException
    }

    @Test
    @DisplayName("salvarUsuario: Deve inserir o usuário no cache com sucesso")
    void salvarUsuario_Sucesso() {
        UsuarioDTO usuarioValido = new UsuarioDTO(USUARIO_ID, "Arthur Carvalho", "arthur@teste.com");
        when(cacheManager.getCache(CACHE_NOME)).thenReturn(cache);

        redisCacheService.salvarUsuario(usuarioValido);

        verify(cacheManager, times(1)).getCache(CACHE_NOME);
        verify(cache, times(1)).put(USUARIO_ID, usuarioValido);
    }

    @Test
    @DisplayName("buscarUsuario: Deve retornar nulo e cobrir a linha final se o cache não existir")
    void buscarUsuario_RetornaNuloSeCacheInexistente() {
        when(cacheManager.getCache(CACHE_NOME)).thenReturn(null);

        UsuarioDTO resultado = redisCacheService.buscarUsuario(USUARIO_ID);

        assertNull(resultado, "O resultado deve ser nulo pois o cache não foi inicializado.");
        verify(cacheManager, times(1)).getCache(CACHE_NOME);
    }

    @Test
    @DisplayName("buscarUsuario: Deve resgatar o usuário do Redis com sucesso")
    void buscarUsuario_Sucesso() {
        UsuarioDTO usuarioValido = new UsuarioDTO(USUARIO_ID, "Arthur Carvalho", "arthur@teste.com");
        when(cacheManager.getCache(CACHE_NOME)).thenReturn(cache);
        when(cache.get(USUARIO_ID, UsuarioDTO.class)).thenReturn(usuarioValido);

        UsuarioDTO resultado = redisCacheService.buscarUsuario(USUARIO_ID);

        assertNotNull(resultado);
        assertEquals("Arthur Carvalho", resultado.getNome());
        verify(cacheManager, times(1)).getCache(CACHE_NOME);
        verify(cache, times(1)).get(USUARIO_ID, UsuarioDTO.class);
    }
}