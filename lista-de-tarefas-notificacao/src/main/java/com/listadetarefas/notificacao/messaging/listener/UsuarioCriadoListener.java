package com.listadetarefas.notificacao.messaging.listener;

import com.listadetarefas.notificacao.messaging.event.UsuarioCriadoEvent;
import com.listadetarefas.notificacao.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioCriadoListener {

    private final EmailService emailService;

    @RabbitListener(queues = "usuarios.v1.usuario-criado.enviar-notificacao")
    public void onUsuarioCriado(UsuarioCriadoEvent event) {
        log.info("Recebido evento do RabbitMQ: Novo usuário -> {}", event.email());

        emailService.enviar(
                event.email(),
                event.nome(),
                "BOAS_VINDAS",
                "Bem-vindo ao Sistema de Tarefas!",
                "Olá " + event.nome() + ",<br><br>Sua conta foi criada com sucesso!"
        );
    }
}