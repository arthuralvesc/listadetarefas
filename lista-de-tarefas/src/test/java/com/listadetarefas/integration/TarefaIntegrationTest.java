package com.listadetarefas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.listadetarefas.ListaDeTarefasApplication;
import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.repository.TarefaRepository;
import com.listadetarefas.service.RedisCacheService;
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
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Injetamos a URL do WireMock dinamicamente para o Feign Client usar
@SpringBootTest(classes = ListaDeTarefasApplication.class, properties = {"API_USUARIOS_URL=http://localhost:8083"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWireMock(port = 8081) // O Servidor Falso da API de Usuários
class TarefaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisCacheService redisCacheService;

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
        Objects.requireNonNull(cacheManager.getCache("usuarios")).clear();
        resetAllRequests();
    }

    @Test
    @DisplayName("Caminho Feliz: Deve criar tarefa com API de usuários respondendo 200 OK")
    void devePersistirTarefaComApiUsuariosEstavel() throws Exception {
        TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Dominar Testcontainers", Prioridade.ALTA);
        UsuarioDTO usuarioMock = new UsuarioDTO(USUARIO_ID, "Nome Teste", "teste@teste.com");

        stubFor(WireMock.get(urlEqualTo("/usuarios/" + USUARIO_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(usuarioMock))));

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, tarefaRepository.count());
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
    }

    @Test
    @DisplayName("Resiliência: Deve criar tarefa ativando o Fallback do Redis quando API retornar 500")
    void deveCriarTarefaUsandoFallbackQuandoApiCair() throws Exception {
        TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estudar Circuit Breaker", Prioridade.ALTA);

        UsuarioDTO usuarioSalvoNoCache = new UsuarioDTO(USUARIO_ID, "Arthur (Cache)", "arthur.cache@teste.com");
        redisCacheService.salvarUsuario(usuarioSalvoNoCache);

        stubFor(WireMock.get(urlEqualTo("/usuarios/" + USUARIO_ID))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, tarefaRepository.count());

        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
    }
}