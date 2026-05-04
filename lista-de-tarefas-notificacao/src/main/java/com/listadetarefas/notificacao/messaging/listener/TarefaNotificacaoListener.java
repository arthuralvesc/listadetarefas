package com.listadetarefas.notificacao.messaging.listener;

import com.listadetarefas.notificacao.messaging.event.NotificacaoTarefaEvent;
import com.listadetarefas.notificacao.service.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class TarefaNotificacaoListener {

    private final SmtpEmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(TarefaNotificacaoListener.class);

    private record ConteudoEmail(String assunto, String corpo) {}

    @RabbitListener(queuesToDeclare = @org.springframework.amqp.rabbit.annotation.Queue(name = "fila.notificacoes", durable = "true"))
    public void processarNotificacaoTarefa(NotificacaoTarefaEvent evento) {
        logger.info("Recebendo evento do RabbitMQ: [{}] para a tarefa '{}'", evento.tipoEvento(), evento.tituloTarefa());

        ConteudoEmail conteudo = montarConteudoDaMensagem(evento);

        if (conteudo == null) {
            return;
        }

        try{
            executarEnvio(evento, conteudo);
        } catch (RuntimeException e) {
            logger.error("Erro ao processar notificacao de nova tarefa para {}", evento.emailDestinatario(), e);
            throw new RuntimeException(e);
        }

    }
    private ConteudoEmail montarConteudoDaMensagem(NotificacaoTarefaEvent evento) {
        return switch (evento.tipoEvento()) {
            case "TAREFA_CRIADA" -> new ConteudoEmail(
                    "Nova Tarefa Criada: " + evento.tituloTarefa(),
                    String.format("Olá %s,<br><br>Sua tarefa <strong>'%s'</strong> foi criada com sucesso e já está disponível.",
                            evento.nomeUsuario(), evento.tituloTarefa())
            );
            case "TAREFA_ATUALIZADA" -> new ConteudoEmail(
                    "Tarefa Atualizada: " + evento.tituloTarefa(),
                    String.format("Olá %s,<br><br>A tarefa <strong>'%s'</strong> sofreu alterações recentes.",
                            evento.nomeUsuario(), evento.tituloTarefa())
            );
            case "TAREFA_DELETADA" -> new ConteudoEmail(
                    "Tarefa Removida: " + evento.tituloTarefa(),
                    String.format("Olá %s,<br><br>A tarefa <strong>'%s'</strong> foi excluída do seu painel.",
                            evento.nomeUsuario(), evento.tituloTarefa())
            );
            default -> {
                logger.warn("Tipo de evento desconhecido ignorado: {}", evento.tipoEvento());
                yield null;
            }
        };
    }

    private void executarEnvio(NotificacaoTarefaEvent evento, ConteudoEmail conteudo) {
        try {
            emailService.enviar(
                    evento.emailDestinatario(),
                    evento.nomeUsuario(),
                    evento.tipoEvento(),
                    conteudo.assunto(),
                    conteudo.corpo()
            );
            logger.info("Processamento finalizado com sucesso para: {}", evento.emailDestinatario());

        } catch (Exception e) {
            logger.error("Erro fatal ao processar a notificação para a tarefa '{}'", evento.tituloTarefa(), e);
        }
    }
}