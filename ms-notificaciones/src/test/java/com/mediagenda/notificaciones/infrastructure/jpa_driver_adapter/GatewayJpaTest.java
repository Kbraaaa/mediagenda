package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.Datos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.infrastructure.mapper.EntidadesMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayJpaTest {

    private final EntidadesMapper mapper = new EntidadesMapper();

    @SuppressWarnings("unchecked")
    @Test
    void gateway() {
        NotificacionRepositoryJPA repo = mock(NotificacionRepositoryJPA.class);
        NotificacionGatewayImpl gateway = new NotificacionGatewayImpl(repo, mapper);
        NotificacionEntity entidad = mapper.toEntity(Datos.notificacion(EstadoNotificacion.FALLIDA));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.findByCodigo("NOT-2026-000001")).thenReturn(Optional.of(entidad));
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entidad), PageRequest.of(0, 10), 1));
        when(repo.existsByEventoId("evt-1")).thenReturn(true);
        when(repo.findTop50ByEstadoAndIntentosLessThanOrderByFechaCreacion(EstadoNotificacion.FALLIDA, 3))
                .thenReturn(List.of(entidad));
        when(repo.siguienteConsecutivo()).thenReturn(4L);

        Notificacion original = Datos.notificacion(EstadoNotificacion.ENVIADA);
        assertThat(gateway.guardar(original)).usingRecursiveComparison().isEqualTo(original);
        assertThat(gateway.buscarPorCodigo("NOT-2026-000001")).isPresent();
        assertThat(gateway.listar(new FiltroNotificaciones(null, null), 0, 10).totalElementos()).isEqualTo(1);
        assertThat(gateway.existeEvento("evt-1")).isTrue();
        assertThat(gateway.fallidasParaReintento(3)).hasSize(1);
        assertThat(gateway.siguienteConsecutivo()).isEqualTo(4L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void especificaciones() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Especificaciones.notificaciones(new FiltroNotificaciones("PAC-000001", EstadoNotificacion.ENVIADA))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), cb);
        verify(cb, times(2)).equal(any(), any(Object.class));
        verify(cb).and(any(Predicate[].class));

        CriteriaBuilder vacio = mock(CriteriaBuilder.class);
        Especificaciones.notificaciones(new FiltroNotificaciones(null, null))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), vacio);
        verify(vacio, never()).equal(any(), any(Object.class));
    }
}
