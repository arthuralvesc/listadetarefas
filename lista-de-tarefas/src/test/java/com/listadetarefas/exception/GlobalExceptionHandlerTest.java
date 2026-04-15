package com.listadetarefas.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve tratar erros de validação e retornar 400 Bad Request")
    void tratarValidacao() {
        MethodArgumentNotValidException exMock = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        FieldError erro = new FieldError("tarefaCreateRequestDTO", "nome", "O nome da tarefa é obrigatório.");

        when(exMock.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(erro));

        ResponseEntity<ErroResposta> response = handler.tratarErrosDeValidacao(exMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro de validação nos campos enviados.", response.getBody().mensagem());
        assertTrue(response.getBody().detalhes().contains("nome: O nome da tarefa é obrigatório."));
    }

    @Test
    @DisplayName("Deve tratar TarefaNaoEncontradaException e retornar 404 Not Found")
    void tratarNaoEncontrada() {
        TarefaNaoEncontradaException ex = new TarefaNaoEncontradaException(100L, 1L);

        ResponseEntity<ErroResposta> response = handler.tratarTarefaNaoEncontrada(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("A tarefa com ID 100 não foi encontrada para o usuário 1.", response.getBody().mensagem());
        assertNull(response.getBody().detalhes());
    }
}