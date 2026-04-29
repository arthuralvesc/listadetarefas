package com.listadetarefas.notificacao.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration
public class MongoConfig {

    // Lê a variável de ambiente crua, ignorando qualquer arquivo properties
    @Value("${SPRING_DATA_MONGODB_URI}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        log.warn("=============================================================");
        log.warn("ASSUMINDO O CONTROLE MANUAL DO MONGODB!");
        log.warn("URI LIDA DIRETO DO DOCKER: {}", mongoUri);
        log.warn("=============================================================");

        // Força a criação do cliente exatamente com a URI do Docker
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        // Vincula o cliente ao nome exato do seu banco de dados
        return new MongoTemplate(mongoClient(), "notificacoes_db");
    }
}