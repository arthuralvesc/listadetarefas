package com.listadetarefas;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Desativado porque já temos o TarefaIntegrationTest validando o contexto com Testcontainers")
class ListaDeTarefasApplicationTests {

    @Test
    void contextLoads() {
    }
}
