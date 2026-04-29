package com.listadetarefas.notificacao;

import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
@Generated
public class ListaDeTarefasNotificacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListaDeTarefasNotificacaoApplication.class, args);
    }

}
