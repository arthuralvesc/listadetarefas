package com.listadetarefas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.ListaDeTarefasApplication;
import com.listadetarefas.client.UsuarioClient;
import com.listadetarefas.config.RabbitMQConfig; // Ajuste o import conforme sua classe de constantes
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.repository.TarefaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ListaDeTarefasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TarefaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private UsuarioClient usuarioClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Value("${api.security.token.secret}")
    private String secret;

    private String tokenValido;
    private final Long USUARIO_ID = 1L;

    @BeforeEach
    void setup() {
        tokenValido = Jwts.builder()
                .claim("id", USUARIO_ID)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        tarefaRepository.deleteAll();

        Objects.requireNonNull(cacheManager.getCache("tarefas")).clear();
    }

    @Test
    @DisplayName("Deve criar tarefa salvando no Postgres, consultando Feign e enviando para o RabbitMQ")
    void devePersistirTarefaENotificar() throws Exception {
        TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Dominar Testcontainers", Prioridade.ALTA);

        UsuarioDTO usuarioMock = new UsuarioDTO(USUARIO_ID, "Nome Teste", "teste@teste.com");
        when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, tarefaRepository.count());
        assertEquals("Dominar Testcontainers", tarefaRepository.findAll().getFirst().getNome());

        verify(usuarioClient, times(1)).buscarUsuarioPorId(USUARIO_ID);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.FILA_NOTIFICACOES),
                any(NotificacaoTarefaEvent.class)
        );
    }

    @Test
    @DisplayName("Deve buscar tarefas no Postgres e armazenar o resultado no Cache Redis")
    void deveBuscarTarefasEGuardarNoRedis() throws Exception {
        TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estudar Arquitetura", Prioridade.MEDIA);
        when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(new UsuarioDTO(USUARIO_ID, "Teste", "teste@teste"));

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertNull(Objects.requireNonNull(cacheManager.getCache("tarefas")).get(USUARIO_ID));

        mockMvc.perform(get("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertNotNull(Objects.requireNonNull(cacheManager.getCache("tarefas")).get(USUARIO_ID),
                "O cache deveria conter a lista de tarefas do usuário após o GET");
    }
}