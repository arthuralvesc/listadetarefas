package com.listadetarefas.service;

import com.listadetarefas.client.UsuarioClient;
import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
import com.listadetarefas.dto.UsuarioDTO;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.exception.TarefaNaoEncontradaException;
import com.listadetarefas.model.Tarefa;
import com.listadetarefas.repository.TarefaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository repository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TarefaService service;

    private Tarefa tarefaMock;
    private final Long USUARIO_ID = 1L;

    private final UsuarioDTO usuarioMock = new UsuarioDTO(USUARIO_ID, "Arthur", "arthur@test.com");

    @BeforeEach
    void setup() {
        tarefaMock = new Tarefa(100L, "Estudar Java", Prioridade.ALTA, Status.NAO_CONCLUIDA, USUARIO_ID, LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("Criação de Tarefa")
    class Criacao {
        @Test
        @DisplayName("Deve salvar, retornar a nova tarefa e enviar evento para o RabbitMQ")
        void deveCriarTarefa() {
            TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estudar Java", Prioridade.ALTA);
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaResponseDTO response = service.criarTarefa(request, USUARIO_ID);

            assertNotNull(response);
            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.NAO_CONCLUIDA, response.status());

            verify(repository, times(1)).save(any(Tarefa.class));

            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
        }
    }

    @Nested
    @DisplayName("Busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar o DTO da tarefa quando ela for encontrada")
        void deveBuscarPorIdComSucesso() {
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.of(tarefaMock));

            TarefaResponseDTO response = service.buscarPorId(100L, USUARIO_ID);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals("Estudar Java", response.nome());
        }

        @Test
        @DisplayName("Deve retornar exceção específica se a tarefa não existir")
        void deveRetornarTarefaNaoEncontrada(){
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.empty());

            assertLancaTarefaNaoEncontrada(() -> service.buscarPorId(100L, USUARIO_ID));
        }
    }

    @Nested
    @DisplayName("Busca com Filtros Dinâmicos")
    class BuscaDinamica {

        @Test
        @DisplayName("Deve buscar por Status E Prioridade")
        void buscarPorStatusEPrioridade() {
            when(repository.findByUsuarioIdAndStatusAndPrioridade(USUARIO_ID, Status.NAO_CONCLUIDA, Prioridade.ALTA))
                    .thenReturn(List.of(tarefaMock));

            List<TarefaResponseDTO> result = service.buscarTarefasPorStatusOuPrioridade(USUARIO_ID, Status.NAO_CONCLUIDA, Prioridade.ALTA);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve buscar apenas por Status")
        void buscarPorStatus() {
            when(repository.findByUsuarioIdAndStatus(USUARIO_ID, Status.NAO_CONCLUIDA)).thenReturn(List.of(tarefaMock));

            List<TarefaResponseDTO> result = service.buscarTarefasPorStatusOuPrioridade(USUARIO_ID, Status.NAO_CONCLUIDA, null);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve buscar apenas por Prioridade")
        void buscarPorPrioridade() {
            when(repository.findByUsuarioIdAndPrioridade(USUARIO_ID, Prioridade.ALTA)).thenReturn(List.of(tarefaMock));

            List<TarefaResponseDTO> result = service.buscarTarefasPorStatusOuPrioridade(USUARIO_ID, null, Prioridade.ALTA);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve buscar todos do usuário quando não houver filtros")
        void buscarSemFiltros() {
            when(repository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(tarefaMock));

            List<TarefaResponseDTO> result = service.buscarTarefasPorStatusOuPrioridade(USUARIO_ID, null, null);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Atualização Parcial e Validações")
    class Atualizacao {

        @Test
        @DisplayName("Deve ignorar nome vazio ou nulo na atualização parcial e notificar")
        void deveIgnorarNomeVazio() {
            when(repository.findById(100L)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("   ", Status.CONCLUIDA, null, USUARIO_ID);

            TarefaResponseDTO response = service.atualizarTarefa(100L, request, USUARIO_ID);

            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.CONCLUIDA, response.status());

            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
        }

        @Test
        @DisplayName("Deve lançar Exceção se o ID não for do usuário")
        void deveFalharSeNaoForDono() {
            when(repository.findById(999L)).thenReturn(Optional.of(tarefaMock));
            tarefaMock.setUsuarioId(2L);
            assertThrows(RuntimeException.class, () -> {
                service.atualizarTarefa(999L, new TarefaUpdateRequestDTO("Teste", null, null, USUARIO_ID), USUARIO_ID);
            });

            verify(rabbitTemplate, never()).convertAndSend(anyString(), any(NotificacaoTarefaEvent.class));
        }

        @Test
        @DisplayName("Deve atualizar nome e prioridade validos, ignorando status nulo")
        void deveAtualizarNomeEPrioridade() {
            when(repository.findById(100L)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("Nome Atualizado", null, Prioridade.BAIXA, USUARIO_ID);

            service.atualizarTarefa(100L, request, USUARIO_ID);

            assertEquals("Nome Atualizado", tarefaMock.getNome());
            assertEquals(Prioridade.BAIXA, tarefaMock.getPrioridade());
            verify(repository).save(tarefaMock);
        }

        @Test
        @DisplayName("Deve ignorar nome nulo na atualização parcial")
        void deveIgnorarNomeNulo() {
            when(repository.findById(100L)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO(null, Status.CONCLUIDA, null, USUARIO_ID);

            TarefaResponseDTO response = service.atualizarTarefa(100L, request, USUARIO_ID);

            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.CONCLUIDA, response.status());
            verify(repository).save(tarefaMock);
        }

        @Test
        @DisplayName("Deve retornar exceção específica se a tarefa não existir")
        void deveRetornarTarefaNaoEncontrada(){
            when(repository.findById(100L)).thenReturn(Optional.empty());

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("Nome Atualizado", null, Prioridade.BAIXA, USUARIO_ID);

            assertLancaTarefaNaoEncontrada(() -> service.atualizarTarefa(100L, request, USUARIO_ID));
        }
    }

    @Nested
    @DisplayName("Exclusão")
    class Exclusao {
        @Test
        @DisplayName("Deve deletar a tarefa se ela existir e notificar a exclusão")
        void deveDeletar() {
            when(repository.findById(100L)).thenReturn(Optional.of(tarefaMock));
            doNothing().when(repository).delete(tarefaMock);

            // NOVO
            when(usuarioClient.buscarUsuarioPorId(USUARIO_ID)).thenReturn(usuarioMock);

            service.deletarTarefa(100L, USUARIO_ID);

            verify(repository, times(1)).delete(tarefaMock);

            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
        }
        @Test
        @DisplayName("Deve negar o acesso a tarefa se o usuario não for o correto")
        void deveNegar() {
            when(repository.findById(999L)).thenReturn(Optional.of(tarefaMock));
            tarefaMock.setUsuarioId(2L);

            assertThrows(RuntimeException.class, () -> {
                service.deletarTarefa(999L, USUARIO_ID);
            });

            verify(rabbitTemplate, never()).convertAndSend(anyString(), any(NotificacaoTarefaEvent.class));
        }

        @Test
        @DisplayName("Deve retornar exceção específica se a tarefa não existir")
        void deveRetornarTarefaNaoEncontrada(){
            when(repository.findById(100L)).thenReturn(Optional.empty());

            assertLancaTarefaNaoEncontrada(() -> service.deletarTarefa(100L, USUARIO_ID));
        }
    }

    private void assertLancaTarefaNaoEncontrada(org.junit.jupiter.api.function.Executable acaoService) {
        assertThrows(TarefaNaoEncontradaException.class, acaoService);
    }
}