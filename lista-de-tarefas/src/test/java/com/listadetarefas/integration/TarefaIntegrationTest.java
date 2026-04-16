package com.listadetarefas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listadetarefas.ListaDeTarefasApplication;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.repository.TarefaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ListaDeTarefasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TarefaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Value("${api.security.token.secret}")
    private String secret;

    private String tokenValido;

    @BeforeEach
    void setup() {
        tokenValido = Jwts.builder()
                .claim("id", 1L)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("H2: Fluxo de persistência real em memória")
    void devePersistirTarefaNoH2() throws Exception {
        TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estudar Integração com H2", Prioridade.ALTA);

        // 1. Faz a chamada passando por todas as camadas
        mockMvc.perform(post("/tarefas")
                        .header("Authorization", "Bearer " + tokenValido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, tarefaRepository.count());
        assertEquals("Estudar Integração com H2", tarefaRepository.findAll().getFirst().getNome());
    }
}