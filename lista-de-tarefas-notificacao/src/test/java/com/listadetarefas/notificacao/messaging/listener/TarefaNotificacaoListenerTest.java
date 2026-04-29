package com.listadetarefas.notificacao.messaging.listener;

import com.listadetarefas.notificacao.messaging.event.NotificacaoTarefaEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.listadetarefas.notificacao.service.SmtpEmailService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaNotificacaoListenerTest {

    @Mock
    private SmtpEmailService emailService;

    @InjectMocks
    private TarefaNotificacaoListener listener;

    // NOVO: Usaremos Captors para "capturar" o texto exato que o listener mandou para o serviço de email
    @Captor
    private ArgumentCaptor<String> assuntoCaptor;

    @Captor
    private ArgumentCaptor<String> corpoCaptor;

    @Nested
    @DisplayName("Processamento de Eventos Conhecidos")
    class EventosConhecidos {

        @Test
        @DisplayName("Deve montar e enviar email para TAREFA_CRIADA")
        void deveProcessarTarefaCriada() {
            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent("arthur@teste.com", "Arthur","TAREFA_CRIADA", "Aprender RabbitMQ");

            listener.processarNotificacaoTarefa(evento);

            verify(emailService, times(1)).enviar(
                    eq("arthur@teste.com"),
                    eq("Arthur"),
                    eq("TAREFA_CRIADA"),
                    assuntoCaptor.capture(),
                    corpoCaptor.capture()
            );

            assertTrue(assuntoCaptor.getValue().contains("Nova Tarefa Criada"));
            assertTrue(corpoCaptor.getValue().contains("foi criada com sucesso"));
        }

        @Test
        @DisplayName("Deve montar e enviar email para TAREFA_ATUALIZADA")
        void deveProcessarTarefaAtualizada() {
            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent("arthur@teste.com", "Arthur","TAREFA_ATUALIZADA", "Aprender RabbitMQ");

            listener.processarNotificacaoTarefa(evento);

            verify(emailService, times(1)).enviar(any(), any(), any(), assuntoCaptor.capture(), corpoCaptor.capture());

            assertTrue(assuntoCaptor.getValue().contains("Tarefa Atualizada"));
            assertTrue(corpoCaptor.getValue().contains("sofreu alterações recentes"));
        }

        @Test
        @DisplayName("Deve montar e enviar email para TAREFA_DELETADA")
        void deveProcessarTarefaDeletada() {
            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent("arthur@teste.com", "Arthur","TAREFA_DELETADA", "Aprender RabbitMQ");

            listener.processarNotificacaoTarefa(evento);

            verify(emailService, times(1)).enviar(any(), any(), any(), assuntoCaptor.capture(), corpoCaptor.capture());

            assertTrue(assuntoCaptor.getValue().contains("Tarefa Removida"));
            assertTrue(corpoCaptor.getValue().contains("foi excluída do seu painel"));
        }
    }

    @Nested
    @DisplayName("Cenários de Exceção e Eventos Desconhecidos")
    class CenariosDeFalha {

        @Test
        @DisplayName("Deve ignorar eventos desconhecidos sem estourar erro")
        void deveIgnorarEventoDesconhecido() {
            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent("arthur@teste.com", "Arthur","EVENTO_FANTASMA", "Aprender RabbitMQ");

            listener.processarNotificacaoTarefa(evento);

            verify(emailService, never()).enviar(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("O try-catch deve impedir que o erro do SmtpEmailService quebre o listener")
        void deveEngolirExcecaoDoEmailService() {
            NotificacaoTarefaEvent evento = new NotificacaoTarefaEvent("arthur@teste.com", "Arthur","TAREFA_CRIADA", "Aprender RabbitMQ");

            doThrow(new RuntimeException("Servidor SMTP Fora do Ar"))
                    .when(emailService).enviar(any(), any(), any(), any(), any());

            listener.processarNotificacaoTarefa(evento);

            verify(emailService, times(1)).enviar(any(), any(), any(), any(), any());
        }
    }
}