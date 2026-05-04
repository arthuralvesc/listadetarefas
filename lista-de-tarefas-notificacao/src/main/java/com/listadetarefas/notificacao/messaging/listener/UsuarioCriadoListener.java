package com.listadetarefas.notificacao.messaging.listener;

import com.listadetarefas.notificacao.messaging.event.UsuarioCriadoEvent;
import com.listadetarefas.notificacao.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioCriadoListener {

    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(UsuarioCriadoListener.class);

    @RabbitListener(queues = "usuarios.v1.usuario-criado.enviar-notificacao")
    public void onUsuarioCriado(UsuarioCriadoEvent event) {
        logger.info("Recebido evento do RabbitMQ: Novo usuário -> {}", event.email());

        try {
            emailService.enviar(
                    event.email(),
                    event.nome(),
                    "BOAS_VINDAS",
                    "Bem-vindo ao Sistema de Tarefas!",
                    "Olá " + event.nome() + ",<br><br>Sua conta foi criada com sucesso!"
            );
        } catch (RuntimeException e) {
            logger.error("Erro ao processar evento de novo usuário para {}", event.email(), e);
            throw new RuntimeException(e);
        }
    }
}