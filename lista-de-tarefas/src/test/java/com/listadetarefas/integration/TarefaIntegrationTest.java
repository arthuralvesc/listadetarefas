package com.listadetarefas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import com.listadetarefas.ListaDeTarefasApplication;
import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ListaDeTarefasApplication.class, properties = {"API_USUARIOS_URL=http://localhost:8083"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWireMock(port = 8083)
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

    @Test
    @DisplayName("Fluxo de Paginação: Deve navegar entre páginas usando o Cursor no banco real")
    void devePaginarTarefasComCursorEndToEnd() throws Exception {
        /*
         * ARRANGE: Populamos o banco com 3 tarefas simulando um volume real de dados.
         * Salvar em lista (saveAll) geralmente garante inserção e ordenação por ID e Timestamp correta no Postgres.
         * Nota: Ajuste a instanciação da 'Tarefa' abaixo de acordo com o construtor real da sua entidade.
         */
        Tarefa t1 = new Tarefa(); t1.setNome("Tarefa Antiga"); t1.setStatus(Status.CONCLUIDA); t1.setPrioridade(Prioridade.BAIXA); t1.setUsuarioId(USUARIO_ID);
        Tarefa t2 = new Tarefa(); t2.setNome("Tarefa Intermediaria"); t2.setStatus(Status.NAO_CONCLUIDA); t2.setPrioridade(Prioridade.MEDIA); t2.setUsuarioId(USUARIO_ID);
        Tarefa t3 = new Tarefa(); t3.setNome("Tarefa Mais Recente"); t3.setStatus(Status.NAO_CONCLUIDA); t3.setPrioridade(Prioridade.ALTA); t3.setUsuarioId(USUARIO_ID);

        tarefaRepository.saveAll(List.of(t1, t2, t3));

        /*
         * ACT 1: O Frontend solicita a primeira página, definindo limite de 2 tarefas.
         * Esperamos receber as 2 tarefas mais recentes e um Cursor gerado pela API.
         */
        MvcResult resultPage1 = mockMvc.perform(get("/tarefas")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true)) // Tem que ter próxima página
                .andExpect(jsonPath("$.nextCursor").exists()) // Cursor não pode ser nulo
                .andReturn();

        // Extraímos a string base64 do Cursor do corpo do JSON da resposta
        String responseBody = resultPage1.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String cursorRecebido = JsonPath.read(responseBody, "$.nextCursor");

        /*
         * ACT 2: O Frontend atinge o fim da tela e solicita a segunda página enviando o Cursor opaco.
         * Esperamos receber a 1 tarefa restante e a indicação de que a lista acabou.
         */
        mockMvc.perform(get("/tarefas")
                        .param("size", "2")
                        .param("cursor", cursorRecebido)
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1)) // Retornou apenas a última tarefa que faltava
                .andExpect(jsonPath("$.hasNext").value(false)) // O scroll chegou ao fim
                .andExpect(jsonPath("$.nextCursor").doesNotExist()); // Valida o @JsonInclude(NON_NULL)
    }
}