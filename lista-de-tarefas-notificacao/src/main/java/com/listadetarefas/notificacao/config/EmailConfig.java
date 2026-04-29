package com.listadetarefas.notificacao.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {
    // Vazio por enquanto. O Spring autoconfigura o JavaMailSender via application.properties.
    // Exemplo do que iria no properties:
    // spring.mail.host=smtp.gmail.com
    // spring.mail.port=587
    // spring.mail.username=seu-email
    // spring.mail.password=sua-senha-de-app
}