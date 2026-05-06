package com.listadetarefas.service;


import com.listadetarefas.client.UsuarioClient;
import com.listadetarefas.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioIntegrationServiceTest {

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private UsuarioIntegrationService usuarioIntegrationService;

    private final Long USUARIO_ID = 1L;


    @Test
    @DisplayName("Deve buscar usuário e salvar no cache em caso de sucesso")
    void deveBuscarESalvarNoCacheComSucesso() {
        UsuarioDTO usuarioMock = new UsuarioDTO(USUARIO_ID, "Arthur", "arthur@teste.com");
        when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

        UsuarioDTO resultado = usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID);

        assertNotNull(resultado);
        assertEquals("Arthur", resultado.getNome());

        verify(redisCacheService, times(1)).salvarUsuario(usuarioMock);
    }

    @Test
    @DisplayName("Fallback: Deve resgatar do Redis quando o Cache existe")
    void deveAcionarFallbackEResgatarDoRedis() {
        UsuarioDTO usuarioCache = new UsuarioDTO(USUARIO_ID, "Arthur Cache", "cache@teste.com");

        Exception erroSimulado = new RuntimeException("Connection Refused");

        when(redisCacheService.buscarUsuario(USUARIO_ID)).thenReturn(usuarioCache);

        // Chamamos diretamente o método de Fallback para garantir a cobertura do JaCoCo
        UsuarioDTO resultado = usuarioIntegrationService.fallbackBuscarUsuario(USUARIO_ID, erroSimulado);

        assertEquals("Arthur Cache", resultado.getNome());
        verify(redisCacheService, times(1)).buscarUsuario(USUARIO_ID);
    }

    @Test
    @DisplayName("Fallback: Deve retornar usuário anônimo quando Redis estiver vazio")
    void deveRetornarAnonimoQuandoCacheFalhar() {
        Exception erroSimulado = new RuntimeException("Timeout Exception");

        when(redisCacheService.buscarUsuario(USUARIO_ID)).thenReturn(null);

        UsuarioDTO resultado = usuarioIntegrationService.fallbackBuscarUsuario(USUARIO_ID, erroSimulado);

        assertEquals("Usuário Indisponível", resultado.getNome());
        assertEquals("email.padrao@listadetarefas.com", resultado.getEmail());
    }
}