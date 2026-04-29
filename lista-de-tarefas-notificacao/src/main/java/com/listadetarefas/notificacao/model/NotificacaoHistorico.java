package com.listadetarefas.notificacao.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "historico_notificacoes")
public class NotificacaoHistorico {

    @Id
    private String id;

    private String destinatario;
    private String nomeUsuario;
    private String tipoEvento;
    private String status;
    private LocalDateTime dataEnvio;
    private String logErro;
}