package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Verifica que cada filtro agregue (o no) su condicion a la consulta. */
class EspecificacionesTest {

    @SuppressWarnings("unchecked")
    @Test
    void citasConTodosLosFiltros() {
        Root<CitaEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Especificaciones.citas(new FiltroCitas("PAC-000001", "MED-0001", EstadoCita.AGENDADA))
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb, times(3)).equal(any(), any(Object.class));
        verify(cb).equal(any(), eq(EstadoCita.AGENDADA));
        verify(cb).and(any(Predicate[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void citasSinFiltros() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Especificaciones.citas(new FiltroCitas(null, null, null))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), cb);

        verify(cb, never()).equal(any(), any(Object.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void franjasConYSinFiltros() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Especificaciones.franjas(new FiltroFranjas("MED-0001", LocalDate.of(2026, 11, 21), true))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), cb);
        verify(cb).equal(any(), eq(EstadoFranja.DISPONIBLE));
        verify(cb, times(3)).equal(any(), any(Object.class));

        CriteriaBuilder vacio = mock(CriteriaBuilder.class);
        Especificaciones.franjas(new FiltroFranjas(null, null, false))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), vacio);
        verify(vacio, never()).equal(any(), any(Object.class));
    }
}
