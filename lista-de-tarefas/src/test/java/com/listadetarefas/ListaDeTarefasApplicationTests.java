package com.listadetarefas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class ListaDeTarefasApplicationTests {

    @Test
    @DisplayName("Deve carregar o contexto do Spring Boot com sucesso sem erros de injeção")
    void contextLoads() {
    }

    @Test
    @DisplayName("Deve executar o método main com sucesso")
    void deveExecutarMetodoMain() {
        assertDoesNotThrow(() -> {
            ListaDeTarefasApplication.main(new String[]{
                    "--server.port=0",
                    "--spring.profiles.active=test"
            });
        });
    }
}
