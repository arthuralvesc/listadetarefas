package com.listadetarefas.notificacao.integration;


import com.listadetarefas.notificacao.ListaDeTarefasNotificacaoApplication;
import com.listadetarefas.notificacao.messaging.event.NotificacaoTarefaEvent;
import com.listadetarefas.notificacao.messaging.event.UsuarioCriadoEvent;
import com.listadetarefas.notificacao.repository.NotificacaoHistoricoRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@SpringBootTest(classes = ListaDeTarefasNotificacaoApplication.class)
@ActiveProfiles("test")
@Testcontainers
class NotificacaoIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:7.0");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registarPropriedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("SPRING_DATA_MONGODB_URI", mongoDB::getReplicaSetUrl);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificacaoHistoricoRepository repository;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    @DisplayName("Fluxo Completo E2E: Deve ler mensagem de Tarefa da fila, enviar email simulado e salvar log no Mongo")
    void deveProcessarNotificacaoDeTarefaComSucesso() {
        NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent(
                "arthur.arquiteto@teste.com",
                "Arthur",
                "TAREFA_CRIADA",
                "Integrar Testcontainers"
        );

        rabbitTemplate.convertAndSend("fila.notificacoes", evento);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {

            assertEquals(1, repository.count(), "O histórico da notificação não foi salvo no MongoDB");
            assertEquals("SUCESSO", repository.findAll().getFirst().getStatus());
            assertEquals("TAREFA_CRIADA", repository.findAll().getFirst().getTipoEvento());

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        });
    }

    @Test
    @DisplayName("Fluxo Completo E2E: Deve ler mensagem de Usuário Criado da fila e enviar email")
    void deveProcessarNotificacaoDeNovoUsuarioComSucesso() {
        UsuarioCriadoEvent evento = new UsuarioCriadoEvent(
                99L,
                "Arthur Carvalho",
                "boasvindas@teste.com"
        );

        rabbitTemplate.convertAndSend("usuarios.v1.events", "", evento);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(mailSender, times(1)).send(any(MimeMessage.class));

            assertEquals(1, repository.count());
            assertEquals("BOAS_VINDAS", repository.findAll().getFirst().getTipoEvento());
            assertEquals("boasvindas@teste.com", repository.findAll().getFirst().getDestinatario());
        });
    }
}