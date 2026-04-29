package com.listadetarefas.notificacao.service;

import com.listadetarefas.notificacao.model.NotificacaoHistorico;
import com.listadetarefas.notificacao.repository.NotificacaoHistoricoRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final NotificacaoHistoricoRepository repository;

    @Override
    public void enviar(String destinatario, String nomeUsuario, String tipoEvento, String assunto, String corpo) {
        try {
            log.info("Preparando e-mail [{}] para envio: {}", tipoEvento, destinatario);

            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);

            mailSender.send(mensagem);

            repository.save(NotificacaoHistorico.builder()
                    .destinatario(destinatario)
                    .nomeUsuario(nomeUsuario)
                    .tipoEvento(tipoEvento)
                    .status("SUCESSO")
                    .dataEnvio(LocalDateTime.now())
                    .build());

            log.info("E-mail [{}] disparado com sucesso para: {}", tipoEvento, destinatario);

        } catch (Exception e) {
            log.error("Falha ao disparar e-mail [{}] para: {}", tipoEvento, destinatario, e);

            repository.save(NotificacaoHistorico.builder()
                    .destinatario(destinatario)
                    .nomeUsuario(nomeUsuario)
                    .tipoEvento(tipoEvento)
                    .status("ERRO")
                    .logErro(e.getMessage())
                    .dataEnvio(LocalDateTime.now())
                    .build());

            throw new RuntimeException("Erro ao processar envio de email via SMTP", e);
        }
    }
}