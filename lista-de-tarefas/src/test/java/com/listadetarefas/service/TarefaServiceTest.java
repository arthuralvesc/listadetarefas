package com.listadetarefas.service;

import com.listadetarefas.dto.TarefaCreateRequestDTO;
import com.listadetarefas.dto.TarefaResponseDTO;
import com.listadetarefas.dto.TarefaUpdateRequestDTO;
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

    @InjectMocks
    private TarefaService service;

    private Tarefa tarefaMock;
    private final Long USUARIO_ID = 1L;

    @BeforeEach
    void setup() {
        tarefaMock = new Tarefa(100L, "Estudar Java", Prioridade.ALTA, Status.NAO_CONCLUIDA, USUARIO_ID, LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("Criação de Tarefa")
    class Criacao {
        @Test
        @DisplayName("Deve salvar e retornar a nova tarefa com status padrão NAO_CONCLUIDA")
        void deveCriarTarefa() {
            TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estudar Java", Prioridade.ALTA);
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            TarefaResponseDTO response = service.criarTarefa(request, USUARIO_ID);

            assertNotNull(response);
            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.NAO_CONCLUIDA, response.status()); // Regra de negócio validada!
            verify(repository, times(1)).save(any(Tarefa.class));
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
        @DisplayName("Deve ignorar nome vazio ou nulo na atualização parcial")
        void deveIgnorarNomeVazio() {
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("   ", Status.CONCLUIDA, null, USUARIO_ID);

            TarefaResponseDTO response = service.atualizarTarefaParcialmente(100L, USUARIO_ID, request);

            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.CONCLUIDA, response.status());
        }

        @Test
        @DisplayName("Deve lançar TarefaNaoEncontradaException se o ID não for do usuário")
        void deveFalharSeNaoForDono() {
            when(repository.findByIdAndUsuarioId(999L, USUARIO_ID)).thenReturn(Optional.empty());

            assertThrows(TarefaNaoEncontradaException.class, () -> {
                service.buscarPorId(999L, USUARIO_ID);
            });
        }

        @Test
        @DisplayName("Deve atualizar nome e prioridade validos, ignorando status nulo")
        void deveAtualizarNomeEPrioridade() {
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("Nome Atualizado", null, Prioridade.BAIXA, USUARIO_ID);

            service.atualizarTarefaParcialmente(100L, USUARIO_ID, request);

            assertEquals("Nome Atualizado", tarefaMock.getNome());
            assertEquals(Prioridade.BAIXA, tarefaMock.getPrioridade());

            assertEquals(Status.NAO_CONCLUIDA, tarefaMock.getStatus());

            verify(repository).save(tarefaMock);
        }

        @Test
        @DisplayName("Deve ignorar nome nulo na atualização parcial")
        void deveIgnorarNomeNulo() {
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO(null, Status.CONCLUIDA, null, USUARIO_ID);

            TarefaResponseDTO response = service.atualizarTarefaParcialmente(100L, USUARIO_ID, request);

            assertEquals("Estudar Java", response.nome());

            assertEquals(Status.CONCLUIDA, response.status());

            verify(repository).save(tarefaMock);
        }
    }

    @Nested
    @DisplayName("Exclusão")
    class Exclusao {
        @Test
        @DisplayName("Deve deletar a tarefa se ela existir")
        void deveDeletar() {
            when(repository.findByIdAndUsuarioId(100L, USUARIO_ID)).thenReturn(Optional.of(tarefaMock));
            doNothing().when(repository).delete(tarefaMock);

            service.deletarTarefa(100L, USUARIO_ID);

            verify(repository, times(1)).delete(tarefaMock);
        }
    }
}