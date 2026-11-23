package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.DatosPaciente;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.DiaFestivoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendarCitaUseCaseTest {

    private static final String SINTOMAS = "Dolor de cabeza fuerte desde hace dos dias";

    private PacienteGateway pacientes;
    private FranjaGateway franjas;
    private CitaGateway citas;
    private EventoGateway eventos;
    private SolicitudTriageGateway triage;
    private FestivoGateway festivos;
    private AgendarCitaUseCase useCase;

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteGateway.class);
        franjas = mock(FranjaGateway.class);
        citas = mock(CitaGateway.class);
        eventos = mock(EventoGateway.class);
        triage = mock(SolicitudTriageGateway.class);
        festivos = mock(FestivoGateway.class);
        useCase = new AgendarCitaUseCase(pacientes, franjas, citas, eventos, triage,
                new ReglasAgenda(Datos.RELOJ, festivos));
        when(pacientes.buscar("PAC-000001")).thenReturn(Optional.of(Datos.paciente()));
        when(franjas.buscarPorId(10L)).thenReturn(Optional.of(Datos.franjaManana()));
        when(franjas.reservar(10L)).thenReturn(true);
        when(citas.siguienteConsecutivo()).thenReturn(45L);
        when(citas.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void agendaReservaPublicaYPideTriage() {
        Cita cita = useCase.ejecutar(" PAC-000001 ", 10L, "  " + SINTOMAS + " ");

        assertThat(cita.getCodigo()).isEqualTo("CIT-2026-000045");
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.AGENDADA);
        assertThat(cita.getEstadoTriage()).isEqualTo(EstadoTriage.PENDIENTE);
        assertThat(cita.getNombrePaciente()).isEqualTo("Laura Gomez");
        assertThat(cita.getSintomas()).isEqualTo(SINTOMAS);
        assertThat(cita.getFranja().getEstado()).isEqualTo(EstadoFranja.RESERVADA);
        ArgumentCaptor<EventoCita> evento = ArgumentCaptor.forClass(EventoCita.class);
        verify(eventos).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(EventoCita.AGENDADA);
        verify(triage).solicitar("CIT-2026-000045");
    }

    @Test
    void validaLosDatosDeEntrada() {
        assertThatThrownBy(() -> useCase.ejecutar(" ", 10L, SINTOMAS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", null, SINTOMAS))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("idFranja");
        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, "corto"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 10 y 1000");
        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("obligatorio");
        verify(pacientes, never()).buscar(any());
    }

    @Test
    void pacienteInexistente() {
        when(pacientes.buscar("PAC-000404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000404", 10L, SINTOMAS))
                .isInstanceOf(NoEncontradoException.class)
                .extracting("codigoError").isEqualTo("PACIENTE_NO_ENCONTRADO");
    }

    @Test
    void pacienteInactivo() {
        when(pacientes.buscar("PAC-000002"))
                .thenReturn(Optional.of(new DatosPaciente("PAC-000002", "Inactivo", "x@y.co", null, false)));

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000002", 10L, SINTOMAS))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("PACIENTE_INACTIVO");
    }

    @Test
    void franjaInexistente() {
        when(franjas.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 99L, SINTOMAS))
                .isInstanceOf(NoEncontradoException.class)
                .extracting("codigoError").isEqualTo("FRANJA_NO_ENCONTRADA");
    }

    @Test
    void franjaQueYaPaso() {
        when(franjas.buscarPorId(5L)).thenReturn(Optional.of(Datos.franja(5L, Datos.HOY, "09:30")));

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 5L, SINTOMAS))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("FRANJA_VENCIDA");
    }

    @Test
    void medicoInactivo() {
        Franja franja = Datos.franjaManana();
        franja.getMedico().setActivo(false);
        when(franjas.buscarPorId(10L)).thenReturn(Optional.of(franja));

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, SINTOMAS))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("MEDICO_INACTIVO");
    }

    @Test
    void diaFestivo() {
        when(festivos.esFestivo(Datos.MANANA)).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, SINTOMAS))
                .isInstanceOf(DiaFestivoException.class);
        verify(franjas, never()).reservar(anyLong());
    }

    @Test
    void dosPacientesMismaFranja_elSegundoRecibeConflicto() {
        when(franjas.reservar(10L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, SINTOMAS))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("FRANJA_OCUPADA");
        verify(citas, never()).guardar(any());
    }

    @Test
    void siGuardarFallaLiberaLaFranja() {
        when(citas.guardar(any())).thenThrow(new IllegalStateException("base caida"));

        assertThatThrownBy(() -> useCase.ejecutar("PAC-000001", 10L, SINTOMAS))
                .isInstanceOf(IllegalStateException.class);
        verify(franjas).liberar(10L);
        verify(eventos, never()).publicar(any());
        verify(triage, never()).solicitar(any());
    }
}
