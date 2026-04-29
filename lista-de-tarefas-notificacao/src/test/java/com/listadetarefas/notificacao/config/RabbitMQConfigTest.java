package com.listadetarefas.notificacao.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMQConfigTest {

    @Test
    void deveCriarBeansRabbitMQ() {
        RabbitMQConfig config = new RabbitMQConfig();

        FanoutExchange exchange = config.usuariosExchange();
        assertEquals("usuarios.v1.events", exchange.getName());

        Queue queue = config.notificacaoQueue();
        assertEquals("usuarios.v1.usuario-criado.enviar-notificacao", queue.getName());
        assertTrue(queue.isDurable());

        Binding binding = config.binding();
        assertNotNull(binding);
        assertEquals("usuarios.v1.usuario-criado.enviar-notificacao", binding.getDestination());
        assertEquals("usuarios.v1.events", binding.getExchange());

        MessageConverter converter = config.jsonMessageConverter();
        assertInstanceOf(JacksonJsonMessageConverter.class, converter);
    }
}