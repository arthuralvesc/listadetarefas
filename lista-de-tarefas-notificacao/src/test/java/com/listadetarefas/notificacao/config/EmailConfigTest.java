package com.listadetarefas.notificacao.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailConfigTest {

    @Test
    void deveInstanciarConfiguracao() {
        EmailConfig config = new EmailConfig();
        assertNotNull(config);
    }
}