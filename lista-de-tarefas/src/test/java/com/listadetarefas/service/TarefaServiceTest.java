package com.listadetarefas.service;

import com.listadetarefas.config.RabbitMQConfig;
import com.listadetarefas.dto.*;
import com.listadetarefas.event.NotificacaoTarefaEvent;
import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.exception.TarefaNaoEncontradaException;
import com.listadetarefas.model.Tarefa;
import com.listadetarefas.repository.TarefaRepository;
import com.listadetarefas.util.CursorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository repository;

    // 1. Mudança crucial: Mockamos o serviço de Integração (Resiliência) e não o Feign Client puro
    @Mock
    private UsuarioIntegrationService usuarioIntegrationService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CursorUtil cursorUtil;

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

            // 2. Usando o método do novo serviço
            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaResponseDTO response = service.criarTarefa(request, USUARIO_ID);

            assertNotNull(response);
            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.NAO_CONCLUIDA, response.status());

            verify(repository, times(1)).save(any(Tarefa.class));
            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
        }
        @Test
        @DisplayName("Deve capturar exceção e logar erro caso a orquestração da notificação falhe")
        void deveCapturarExcecaoAoOrquestrarNotificacao() {
            // Preparação (Arrange)
            TarefaCreateRequestDTO request = new TarefaCreateRequestDTO("Estruturar backend do Basketpedia", Prioridade.ALTA);
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            // Forçamos o serviço de usuários a estourar um erro no meio da criação da tarefa
            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID))
                    .thenThrow(new RuntimeException("Simulação de falha catastrófica de rede"));

            // Ação (Act)
            TarefaResponseDTO response = service.criarTarefa(request, USUARIO_ID);

            // Verificação (Assert)
            assertNotNull(response, "A tarefa deve ser criada mesmo se a notificação falhar.");
            assertEquals("Estudar Java", response.nome()); // Baseado no seu tarefaMock do setup

            // Garantimos que a mensagem nunca chegou a ser enviada ao RabbitMQ
            verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
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
    @DisplayName("Busca com Filtros Dinâmicos e Paginação (Cursor API)")
    class BuscaDinamica {

        @Test
        @DisplayName("Deve buscar a primeira página com filtros (Cursor Nulo)")
        void buscarPrimeiraPaginaComFiltros() {
            Window<Tarefa> mockWindow = mock(Window.class);
            when(mockWindow.getContent()).thenReturn(List.of(tarefaMock));
            when(mockWindow.hasNext()).thenReturn(false);

            when(repository.findBy(any(Specification.class), any())).thenReturn(mockWindow);

            TarefaCursorResponseDTO response = service.buscarTarefas(USUARIO_ID, Status.NAO_CONCLUIDA, Prioridade.ALTA, null, 10);

            assertNotNull(response);
            assertEquals(1, response.data().size());
            assertFalse(response.hasNext());
            assertNull(response.nextCursor());

            verify(cursorUtil, never()).decodificarCursor(anyString());
        }

        @Test
        @DisplayName("Deve decodificar o cursor e buscar a próxima página")
        void buscarProximaPaginaComCursor() {
            String cursorBase64 = "ZXlKcFpDSTZNVFEz...";
            Map<String, Object> cursorValues = Map.of("id", 100L);

            when(cursorUtil.resolverScrollPosition(cursorBase64)).thenReturn(ScrollPosition.of(cursorValues, ScrollPosition.Direction.FORWARD));
            when(cursorUtil.codificarCursor(anyMap())).thenReturn("NOVO_CURSOR_BASE64");

            Window<Tarefa> mockWindow = mock(Window.class);
            when(mockWindow.getContent()).thenReturn(List.of(tarefaMock));
            when(mockWindow.hasNext()).thenReturn(true);

            KeysetScrollPosition mockPosition = mock(KeysetScrollPosition.class);
            when(mockPosition.getKeys()).thenReturn(cursorValues);
            when(mockWindow.positionAt(0)).thenReturn(mockPosition);

            when(repository.findBy(any(Specification.class), any())).thenReturn(mockWindow);


            TarefaCursorResponseDTO response = service.buscarTarefas(USUARIO_ID, null, null, cursorBase64, 10);

            assertNotNull(response);
            assertTrue(response.hasNext());
            assertEquals("NOVO_CURSOR_BASE64", response.nextCursor());

            verify(cursorUtil, times(1)).resolverScrollPosition(cursorBase64);
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

            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID)).thenReturn(usuarioMock);

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

            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO("Nome Atualizado", null, Prioridade.BAIXA, USUARIO_ID);

            service.atualizarTarefa(100L, request, USUARIO_ID);

            assertEquals("Nome Atualizado", tarefaMock.getNome());
            assertEquals(Prioridade.BAIXA, tarefaMock.getPrioridade());
            verify(repository).save(tarefaMock);

            // Verificação do RabbitMQ adicionada
            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
        }

        @Test
        @DisplayName("Deve ignorar nome nulo na atualização parcial")
        void deveIgnorarNomeNulo() {
            when(repository.findById(100L)).thenReturn(Optional.of(tarefaMock));
            when(repository.save(any(Tarefa.class))).thenReturn(tarefaMock);

            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID)).thenReturn(usuarioMock);

            TarefaUpdateRequestDTO request = new TarefaUpdateRequestDTO(null, Status.CONCLUIDA, null, USUARIO_ID);

            TarefaResponseDTO response = service.atualizarTarefa(100L, request, USUARIO_ID);

            assertEquals("Estudar Java", response.nome());
            assertEquals(Status.CONCLUIDA, response.status());
            verify(repository).save(tarefaMock);

            // Verificação do RabbitMQ adicionada
            verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.FILA_NOTIFICACOES), any(NotificacaoTarefaEvent.class));
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

            when(usuarioIntegrationService.buscarDetalhesUsuario(USUARIO_ID)).thenReturn(usuarioMock);

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