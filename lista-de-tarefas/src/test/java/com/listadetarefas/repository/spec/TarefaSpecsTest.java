package com.listadetarefas.repository.spec;

import com.listadetarefas.model.Prioridade;
import com.listadetarefas.model.Status;
import com.listadetarefas.model.Tarefa;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaSpecsTest {

    @Mock
    private Root<Tarefa> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder builder;

    @Mock
    private Path<Object> path;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
    }

    @Test
    @DisplayName("Deve impedir a instanciação da classe utilitária")
    void construtorPrivado() throws Exception {
        java.lang.reflect.Constructor<TarefaSpecs> constructor = TarefaSpecs.class.getDeclaredConstructor();

        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);

        java.lang.reflect.InvocationTargetException exception = assertThrows(
                java.lang.reflect.InvocationTargetException.class, constructor::newInstance
        );

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Esta é uma classe utilitária e não pode ser instanciada", exception.getCause().getMessage());
    }

    @Test
    void pertenceAoUsuario() {
        Specification<Tarefa> spec = TarefaSpecs.pertenceAoUsuario(1L);
        spec.toPredicate(root, query, builder);

        verify(root).get("usuarioId");
        verify(builder).equal(path, 1L);
    }

    @Test
    void comStatus() {
        Specification<Tarefa> spec = TarefaSpecs.comStatus(Status.NAO_CONCLUIDA);
        spec.toPredicate(root, query, builder);

        verify(root).get("status");
        verify(builder).equal(path, Status.NAO_CONCLUIDA);
    }

    @Test
    void comStatusNulo() {
        Specification<Tarefa> spec = TarefaSpecs.comStatus(null);

        assertNull(spec.toPredicate(root, query, builder));
        verifyNoInteractions(builder);
    }

    @Test
    void comPrioridade() {
        Specification<Tarefa> spec = TarefaSpecs.comPrioridade(Prioridade.ALTA);
        spec.toPredicate(root, query, builder);

        verify(root).get("prioridade");
        verify(builder).equal(path, Prioridade.ALTA);
    }

    @Test
    void comPrioridadeNula() {
        Specification<Tarefa> spec = TarefaSpecs.comPrioridade(null);

        assertNull(spec.toPredicate(root, query, builder));
        verifyNoInteractions(builder);
    }
}