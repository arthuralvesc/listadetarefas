package com.listadetarefas.notificacao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "usuarios.v1.events";
    public static final String QUEUE_NAME = "usuarios.v1.usuario-criado.enviar-notificacao";

    @Bean
    public FanoutExchange usuariosExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificacaoQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(notificacaoQueue()).to(usuariosExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
