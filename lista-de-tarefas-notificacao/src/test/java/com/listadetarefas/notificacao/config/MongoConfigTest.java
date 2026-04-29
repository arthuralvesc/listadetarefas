package com.listadetarefas.notificacao.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoConfigTest {

    @Test
    void deveConfigurarMongo() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "mongoUri", "mongodb://localhost:27017");

        MongoClient client = config.mongoClient();
        assertNotNull(client);

        MongoTemplate template = config.mongoTemplate();
        assertNotNull(template);
        assertEquals("notificacoes_db", template.getDb().getName());
    }
}