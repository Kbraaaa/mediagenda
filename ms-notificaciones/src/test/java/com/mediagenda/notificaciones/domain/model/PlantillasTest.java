package com.mediagenda.notificaciones.domain.model;

import com.mediagenda.notificaciones.Datos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlantillasTest {

    @ParameterizedTest
    @CsvSource({
            "cita.agendada, Cita agendada CIT-2026-000001, Quedo agendada",
            "cita.reprogramada, Cita reprogramada CIT-2026-000001, Reprogramamos",
            "cita.cancelada, Cita cancelada CIT-2026-000001, Se cancelo",
            "cita.recordatorio, Recordatorio: su cita es pronto, Le recordamos",
            "cita.triage.completado, Orientacion sobre su cita CIT-2026-000001, detalle del evento"})
    void avisosDeCitas(String tipo, String asunto, String fragmento) {
        Aviso aviso = Plantillas.redactar(Datos.eventoCita(tipo));

        assertThat(aviso.asunto()).isEqualTo(asunto);
        assertThat(aviso.cuerpo()).startsWith("Hola Laura Gomez").contains(fragmento).endsWith("MediAgenda IPS");
    }

    @Test
    void laCitaMuestraMedicoFechaYHoraCorta() {
        assertThat(Plantillas.redactar(Datos.eventoCita("cita.agendada")).cuerpo())
                .contains("Andrea Lopez (MEDICINA_GENERAL) el 2026-11-21 a las 08:00.");
    }

    @Test
    void avisosDeFormulas() {
        Aviso emitida = Plantillas.redactar(Datos.eventoFormula("formula.emitida"));
        Aviso entregada = Plantillas.redactar(Datos.eventoFormula("formula.dispensada"));

        assertThat(emitida.asunto()).isEqualTo("Formula FOR-2026-000001 lista");
        assertThat(emitida.cuerpo()).contains("Medicamentos: 20 x Acetaminofen");
        assertThat(entregada.asunto()).isEqualTo("Medicamentos entregados");
    }

    @Test
    void eventoIncompletoODesconocido() {
        EventoRecibido vacio = new EventoRecibido("e", null, null, null, "a@b.co", null, null, null, null, null, "8",
                null, null, null);

        Aviso aviso = Plantillas.redactar(vacio);

        assertThat(aviso.asunto()).isEqualTo("Aviso de MediAgenda");
        assertThat(aviso.cuerpo()).startsWith("Hola paciente").contains("Tiene un nuevo aviso.");
        EventoRecibido sinHora = new EventoRecibido("e", "cita.cancelada", null, " ", "a@b.co", null, null, null, null,
                null, "8", null, null, null);
        assertThat(Plantillas.redactar(sinHora).cuerpo()).contains("a las 8.");
        EventoRecibido formulaSinDatos = new EventoRecibido("e", "formula.dispensada", null, null, "a@b.co", null, null,
                null, null, null, null, null, null, null);
        assertThat(Plantillas.redactar(formulaSinDatos).cuerpo()).contains("Sus medicamentos fueron entregados");
    }

    @Test
    void codigosYPagina() {
        assertThat(Codigos.notificacion(2026, 310)).isEqualTo("NOT-2026-000310");
        assertThatThrownBy(() -> Codigos.notificacion(2026, 0)).isInstanceOf(IllegalArgumentException.class);
        Pagina.validarParametros(0, 10);
        assertThatThrownBy(() -> Pagina.validarParametros(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagina.validarParametros(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Pagina<>(List.of(1), 0, 1, 1, 1).contenido()).hasSize(1);
    }
}
