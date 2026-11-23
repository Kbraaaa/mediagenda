package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reprogramar, cancelar, atender, consultar y listar citas. */
class GestionCitaUseCaseTest {

    private static final String CODIGO = "CIT-2026-000001";

    private CitaGateway citas;
    private FranjaGateway franjas;
    private EventoGateway eventos;
    private ReglasAgenda reglas;
    private Cita cita;

    @BeforeEach
    void preparar() {
        citas = mock(CitaGateway.class);
        franjas = mock(FranjaGateway.class);
        eventos = mock(EventoGateway.class);
        reglas = new ReglasAgenda(Datos.RELOJ, mock(FestivoGateway.class));
        cita = Datos.cita();
        when(citas.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(cita));
        when(citas.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void reprogramaReservandoLaNuevaYLiberandoLaAnterior() {
        when(franjas.buscarPorId(11L)).thenReturn(Optional.of(Datos.franja(11L, Datos.MANANA, "09:00")));
        when(franjas.reservar(11L)).thenReturn(true);
        cita.setRecordatorioEnviado(true);

        Cita resultado = new ReprogramarCitaUseCase(citas, franjas, eventos, reglas).ejecutar(CODIGO, 11L);

        assertThat(resultado.getFranja().getIdFranja()).isEqualTo(11L);
        assertThat(resultado.getFranja().getEstado()).isEqualTo(EstadoFranja.RESERVADA);
        assertThat(resultado.isRecordatorioEnviado()).isFalse();
        verify(franjas).liberar(10L);
        ArgumentCaptor<EventoCita> evento = ArgumentCaptor.forClass(EventoCita.class);
        verify(eventos).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(EventoCita.REPROGRAMADA);
    }

    @Test
    void reprogramarValidaEntradas() {
        ReprogramarCitaUseCase useCase = new ReprogramarCitaUseCase(citas, franjas, eventos, reglas);
        when(franjas.buscarPorId(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, 10L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya esta");
        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, 12L))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void reprogramarAFranjaOcupada() {
        when(franjas.buscarPorId(11L)).thenReturn(Optional.of(Datos.franja(11L, Datos.MANANA, "09:00")));
        when(franjas.reservar(11L)).thenReturn(false);

        assertThatThrownBy(() -> new ReprogramarCitaUseCase(citas, franjas, eventos, reglas).ejecutar(CODIGO, 11L))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("FRANJA_OCUPADA");
        verify(franjas, never()).liberar(10L);
    }

    @Test
    void siGuardarFallaAlReprogramarLiberaLaNueva() {
        when(franjas.buscarPorId(11L)).thenReturn(Optional.of(Datos.franja(11L, Datos.MANANA, "09:00")));
        when(franjas.reservar(11L)).thenReturn(true);
        when(citas.guardar(any())).thenThrow(new IllegalStateException("falla"));

        assertThatThrownBy(() -> new ReprogramarCitaUseCase(citas, franjas, eventos, reglas).ejecutar(CODIGO, 11L))
                .isInstanceOf(IllegalStateException.class);
        verify(franjas).liberar(11L);
        verify(franjas, never()).liberar(10L);
    }

    @Test
    void cancelaYLiberaLaFranja() {
        Cita cancelada = new CancelarCitaUseCase(citas, franjas, eventos).ejecutar(CODIGO);

        assertThat(cancelada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cancelada.getFranja().getEstado()).isEqualTo(EstadoFranja.DISPONIBLE);
        verify(franjas).liberar(10L);
        verify(eventos).publicar(any());
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        cita.setEstado(EstadoCita.CANCELADA);

        assertThatThrownBy(() -> new CancelarCitaUseCase(citas, franjas, eventos).ejecutar(CODIGO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("CITA_NO_AGENDADA");
    }

    @Test
    void atiendeLaCita() {
        assertThat(new AtenderCitaUseCase(citas).ejecutar(CODIGO).getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void citaInexistente() {
        when(citas.buscarPorCodigo("CIT-2026-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AtenderCitaUseCase(citas).ejecutar("CIT-2026-999999"))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> new ConsultarCitaUseCase(citas).ejecutar("CIT-2026-999999"))
                .isInstanceOf(NoEncontradoException.class)
                .extracting("codigoError").isEqualTo("CITA_NO_ENCONTRADA");
    }

    @Test
    void consultaYLista() {
        FiltroCitas filtro = new FiltroCitas("PAC-000001", null, EstadoCita.AGENDADA);
        Pagina<Cita> pagina = new Pagina<>(List.of(cita), 0, 10, 1, 1);
        when(citas.listar(filtro, 0, 10)).thenReturn(pagina);

        assertThat(new ConsultarCitaUseCase(citas).ejecutar(CODIGO)).isSameAs(cita);
        assertThat(new ListarCitasUseCase(citas).ejecutar(filtro, 0, 10)).isSameAs(pagina);
        assertThatThrownBy(() -> new ListarCitasUseCase(citas).ejecutar(filtro, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reglasDeTexto() {
        assertThatThrownBy(() -> ReglasAgenda.validarTexto("x".repeat(30), "campo", 1, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 1 y 20");
        assertThat(reglas.hoy()).isEqualTo(Datos.HOY);
    }
}
