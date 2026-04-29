package com.listadetarefas.notificacao.repository;

import com.listadetarefas.notificacao.model.NotificacaoHistorico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoHistoricoRepository extends MongoRepository<NotificacaoHistorico, String> {
}