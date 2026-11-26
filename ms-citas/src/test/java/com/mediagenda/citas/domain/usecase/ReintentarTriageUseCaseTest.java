package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Volver a pedir el triage con IA cuando fallo. */
class ReintentarTriageUseCaseTest {

    private static final String CODIGO = "CIT-2026-000001";

    private CitaGateway citas;
    private SolicitudTriageGateway solicitud;
    private ReintentarTriageUseCase useCase;
    private Cita cita;

    @BeforeEach
    void preparar() {
        citas = mock(CitaGateway.class);
        solicitud = mock(SolicitudTriageGateway.class);
        useCase = new ReintentarTriageUseCase(citas, solicitud);
        cita = Datos.cita();
        when(citas.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(cita));
    }

    @Test
    void pasaElTriageFallidoAPendienteYLoEncola() {
        cita.setEstadoTriage(EstadoTriage.FALLIDO);

        Cita resultado = useCase.ejecutar(CODIGO);

        assertThat(resultado.getEstadoTriage()).isEqualTo(EstadoTriage.PENDIENTE);
        verify(citas).actualizarTriage(CODIGO, EstadoTriage.PENDIENTE, null);
        verify(solicitud).solicitar(CODIGO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTriage.class, names = {"PENDIENTE", "COMPLETADO"})
    void soloSeReintentaUnTriageFallido(EstadoTriage estado) {
        cita.setEstadoTriage(estado);

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining(estado.name())
                .extracting("codigoError").isEqualTo("TRIAGE_NO_FALLIDO");
        verify(solicitud, never()).solicitar(anyString());
    }

    @Test
    void laCitaDebeEstarAgendada() {
        cita.setEstadoTriage(EstadoTriage.FALLIDO);
        cita.setEstado(EstadoCita.CANCELADA);

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("CITA_NO_AGENDADA");
        verify(citas, never()).actualizarTriage(anyString(), any(), any());
    }

    @Test
    void citaInexistenteEs404() {
        when(citas.buscarPorCodigo("CIT-2026-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("CIT-2026-999999")).isInstanceOf(NoEncontradoException.class);
    }
}
