package com.listadetarefas.usuarios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException exception) {
        List<String> erros = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage() + ",")
                .toList();

        return criarResposta(HttpStatus.BAD_REQUEST, "Erro de validação nos campos", erros);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResposta> tratarEmailDuplicado(EmailJaCadastradoException exception) {
        return criarResposta(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(UsuarioNaoEncontradoException exception) {
        return criarResposta(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroGenerico(Exception exception) {
        exception.printStackTrace();
        return criarResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado.", null);
    }

    private ResponseEntity<ErroResposta> criarResposta(HttpStatus status, String msg, List<String> detalhes) {
        ErroResposta erro = new ErroResposta(LocalDateTime.now(), status.value(), msg, detalhes);
        return ResponseEntity.status(status).body(erro);
    }
}