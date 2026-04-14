package com.listadetarefas.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResposta(
        LocalDateTime timestamp,
        int status,
        String mensagem,
        List<String> detalhes
) {}