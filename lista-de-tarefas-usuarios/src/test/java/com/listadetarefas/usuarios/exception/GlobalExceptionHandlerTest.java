package com.listadetarefas.usuarios.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setup() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("Validação de Campos (400 Bad Request)")
    class ValidacaoDeCampos {

        @Test
        @DisplayName("Deve formatar e retornar os erros de validação do DTO")
        void deveTratarMethodArgumentNotValidException() {
            MethodArgumentNotValidException exceptionMock = Mockito.mock(MethodArgumentNotValidException.class);
            BindingResult bindingResultMock = Mockito.mock(BindingResult.class);

            FieldError erroDeEmail = new FieldError("usuarioRequestDTO", "email", "não pode ser nulo");

            when(exceptionMock.getBindingResult()).thenReturn(bindingResultMock);
            when(bindingResultMock.getFieldErrors()).thenReturn(List.of(erroDeEmail));

            ResponseEntity<ErroResposta> response = exceptionHandler.tratarValidacao(exceptionMock);

            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());

            assertEquals("Erro de validação nos campos", response.getBody().mensagem());
            assertEquals(400, response.getBody().status());
            assertTrue(response.getBody().detalhes().contains("email: não pode ser nulo,"));
        }
    }

    @Nested
    @DisplayName("Regras de Negócio (404 e 409)")
    class RegrasDeNegocio {

        @Test
        @DisplayName("Deve retornar 409 Conflict quando o email já existir")
        void deveTratarEmailDuplicado() {
            String emailDuplicado = "arthur@exemplo.com";
            EmailJaCadastradoException exception = new EmailJaCadastradoException(emailDuplicado);

            ResponseEntity<ErroResposta> response = exceptionHandler.tratarEmailDuplicado(exception);

            assertNotNull(response);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

            assertEquals(409, response.getBody().status());

            String mensagemEsperada = "O email arthur@exemplo.com já está em uso no sistema.";
            assertEquals(mensagemEsperada, response.getBody().mensagem());

            assertNull(response.getBody().detalhes());
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
        void deveTratarUsuarioNaoEncontrado() {
            UsuarioNaoEncontradoException exception = new UsuarioNaoEncontradoException(99L);

            ResponseEntity<ErroResposta> response = exceptionHandler.tratarNaoEncontrado(exception);

            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().status());
            assertEquals(exception.getMessage(), response.getBody().mensagem());
        }
    }

    @Nested
    @DisplayName("Erros Inesperados (500 Internal Server Error)")
    class ErrosInesperados {

        @Test
        @DisplayName("Deve retornar 500 com mensagem genérica para não vazar infraestrutura")
        void deveTratarErroGenerico() {
            Exception exception = new Exception("Falha brutal de conexão com o PostgreSQL");

            ResponseEntity<ErroResposta> response = exceptionHandler.tratarErroGenerico(exception);

            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().status());
            assertEquals("Ocorreu um erro interno inesperado.", response.getBody().mensagem());
        }
    }
}