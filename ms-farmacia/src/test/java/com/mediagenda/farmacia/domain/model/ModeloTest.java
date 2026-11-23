package com.mediagenda.farmacia.domain.model;

import com.mediagenda.farmacia.Datos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloTest {

    @Test
    void codigos() {
        assertThat(Codigos.medicamento(9)).isEqualTo("MDT-0009");
        assertThat(Codigos.formula(2026, 12)).isEqualTo("FOR-2026-000012");
        assertThatThrownBy(() -> Codigos.formula(2026, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void citaAtendida() {
        assertThat(Datos.citaAtendida().atendida()).isTrue();
        assertThat(new DatosCita("C", "P", "N", "c@c.co", null, "AGENDADA").atendida()).isFalse();
    }

    @Test
    void eventoListaLosMedicamentos() {
        EventoFormula evento = EventoFormula.de(EventoFormula.EMITIDA, Datos.formula(), "lista");

        assertThat(evento.tipo()).isEqualTo("formula.emitida");
        assertThat(evento.codigoFormula()).isEqualTo("FOR-2026-000001");
        assertThat(evento.correoPaciente()).isEqualTo("laura@correo.com");
        assertThat(evento.telefonoPaciente()).isEqualTo("3101234567");
        assertThat(evento.medicamentos())
                .isEqualTo("20 x Acetaminofen Tabletas 500 mg; 2 x Ibuprofeno Tabletas 400 mg");
        assertThat(evento.eventoId()).isNotBlank();
    }

    @Test
    void pagina() {
        Pagina.validarParametros(0, 1);
        assertThatThrownBy(() -> Pagina.validarParametros(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagina.validarParametros(0, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Pagina<>(List.of("a"), 0, 1, 1, 1).totalPaginas()).isEqualTo(1);
    }
}
