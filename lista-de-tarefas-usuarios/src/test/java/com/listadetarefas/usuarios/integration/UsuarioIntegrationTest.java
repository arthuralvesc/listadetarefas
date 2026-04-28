package com.listadetarefas.usuarios.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.usuarios.ListaDeTarefasUsuariosApplication;
import com.listadetarefas.usuarios.dto.UsuarioRequestDTO;
import com.listadetarefas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ListaDeTarefasUsuariosApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UsuarioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();

        while (rabbitTemplate.receive("usuarios.v1.usuario-criado.enviar-notificacao") != null) {
        }
    }

    @Test
    @DisplayName("Deve cadastrar usuário no banco e enviar evento para o RabbitMQ")
    void deveCadastrarUsuarioEPublicarMensagem() throws Exception {
        UsuarioRequestDTO request = new UsuarioRequestDTO("Arthur", "arthur@email.com", "senhaSegura123");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, usuarioRepository.count());
        assertEquals("Arthur", usuarioRepository.findAll().getFirst().getNome());

        Object mensagemRecebida = rabbitTemplate.receiveAndConvert(
                "usuarios.v1.usuario-criado.enviar-notificacao", 3000);

        assertNotNull(mensagemRecebida, "A mensagem de notificação não chegou na fila do RabbitMQ!");
    }
}