package com.mediagenda.citas.domain.model;

import com.mediagenda.citas.Datos;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloTest {

    @Test
    void especialidadAceptaEspaciosYMinusculas() {
        assertThat(Especialidad.desde(" medicina general ")).isEqualTo(Especialidad.MEDICINA_GENERAL);
        assertThat(Especialidad.desde("cardiologia")).isEqualTo(Especialidad.CARDIOLOGIA);
    }

    @Test
    void especialidadInvalidaOVacia() {
        assertThatThrownBy(() -> Especialidad.desde(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Especialidad.desde(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Especialidad.desde("astrologia"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("astrologia");
    }

    @Test
    void codigosLegibles() {
        assertThat(Codigos.medico(7)).isEqualTo("MED-0007");
        assertThat(Codigos.cita(2026, 45)).isEqualTo("CIT-2026-000045");
        assertThatThrownBy(() -> Codigos.medico(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paginaValidaParametros() {
        Pagina.validarParametros(0, 100);
        assertThatThrownBy(() -> Pagina.validarParametros(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagina.validarParametros(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagina.validarParametros(0, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Pagina<>(List.of(1), 0, 1, 1, 1).contenido()).containsExactly(1);
    }

    @Test
    void franjaSabeCuandoEmpieza() {
        assertThat(Datos.franjaManana().inicio()).isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 0));
    }

    @Test
    void eventoSeArmaDesdeLaCita() {
        EventoCita evento = EventoCita.de(EventoCita.AGENDADA, Datos.cita(), "detalle");

        assertThat(evento.eventoId()).isNotBlank();
        assertThat(evento.tipo()).isEqualTo("cita.agendada");
        assertThat(evento.codigoCita()).isEqualTo("CIT-2026-000001");
        assertThat(evento.correoPaciente()).isEqualTo("laura@correo.com");
        assertThat(evento.telefonoPaciente()).isEqualTo("3101234567");
        assertThat(evento.medico()).isEqualTo("Andrea Lopez");
        assertThat(evento.especialidad()).isEqualTo("MEDICINA_GENERAL");
        assertThat(evento.fecha()).isEqualTo(Datos.MANANA);
        assertThat(evento.detalle()).isEqualTo("detalle");
        assertThat(EventoCita.de(EventoCita.AGENDADA, Datos.cita(), "x").eventoId()).isNotEqualTo(evento.eventoId());
    }
}
