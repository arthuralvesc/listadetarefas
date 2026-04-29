package com.listadetarefas.notificacao.service;

import com.listadetarefas.notificacao.model.NotificacaoHistorico;
import com.listadetarefas.notificacao.repository.NotificacaoHistoricoRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificacaoHistoricoRepository repository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private SmtpEmailService service;

    @Test
    void deveEnviarEmailComSucesso() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.enviar("teste@teste.com", "User", "EVENTO", "Assunto", "Corpo");

        verify(mailSender).send(mimeMessage);

        ArgumentCaptor<NotificacaoHistorico> captor = ArgumentCaptor.forClass(NotificacaoHistorico.class);
        verify(repository).save(captor.capture());

        assertEquals("SUCESSO", captor.getValue().getStatus());
        assertEquals("teste@teste.com", captor.getValue().getDestinatario());
    }

    @Test
    void deveRegistrarErroQuandoEnvioFalhar() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Offline")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () ->
                service.enviar("teste@teste.com", "User", "EVENTO", "Assunto", "Corpo")
        );

        ArgumentCaptor<NotificacaoHistorico> captor = ArgumentCaptor.forClass(NotificacaoHistorico.class);
        verify(repository).save(captor.capture());

        assertEquals("ERRO", captor.getValue().getStatus());
        assertEquals("SMTP Offline", captor.getValue().getLogErro());
    }
}