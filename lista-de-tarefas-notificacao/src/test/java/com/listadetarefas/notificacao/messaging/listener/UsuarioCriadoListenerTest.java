package com.listadetarefas.notificacao.messaging.listener;

import com.listadetarefas.notificacao.messaging.event.UsuarioCriadoEvent;
import com.listadetarefas.notificacao.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuarioCriadoListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioCriadoListener listener;

    @Test
    @DisplayName("Deve processar evento de novo usuário e disparar email de boas-vindas")
    void deveProcessarUsuarioCriado() {
        UsuarioCriadoEvent evento = new UsuarioCriadoEvent(1L, "Arthur Carvalho", "arthur@test.com");

        // 2. Act
        listener.onUsuarioCriado(evento);

        verify(emailService, times(1)).enviar(
                eq("arthur@test.com"),
                eq("Arthur Carvalho"),
                eq("BOAS_VINDAS"),
                eq("Bem-vindo ao Sistema de Tarefas!"),
                contains("Arthur Carvalho")
        );
    }
}