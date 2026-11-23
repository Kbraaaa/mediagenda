package com.mediagenda.citas;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.DatosPaciente;
import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

/** Datos de prueba. "Hoy" en las pruebas es viernes 20 de noviembre de 2026, 10:00 en Bogota. */
public final class Datos {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));
    public static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    public static final LocalDate MANANA = HOY.plusDays(1);

    private Datos() {
    }

    public static Medico medico() {
        return Medico.builder()
                .idMedico(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                .codigo("MED-0001")
                .nombre("Andrea Lopez")
                .especialidad(Especialidad.MEDICINA_GENERAL)
                .correo("andrea.lopez@mediagenda.co")
                .activo(true)
                .build();
    }

    public static Franja franja(long id, LocalDate fecha, String hora) {
        LocalTime inicio = LocalTime.parse(hora);
        return Franja.builder()
                .idFranja(id)
                .medico(medico())
                .fecha(fecha)
                .horaInicio(inicio)
                .horaFin(inicio.plusMinutes(30))
                .estado(EstadoFranja.DISPONIBLE)
                .build();
    }

    public static Franja franjaManana() {
        return franja(10L, MANANA, "08:00");
    }

    public static DatosPaciente paciente() {
        return new DatosPaciente("PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567", true);
    }

    public static Cita cita() {
        Franja franja = franjaManana();
        franja.setEstado(EstadoFranja.RESERVADA);
        return Cita.builder()
                .idCita(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .codigo("CIT-2026-000001")
                .codigoPaciente("PAC-000001")
                .nombrePaciente("Laura Gomez")
                .correoPaciente("laura@correo.com")
                .telefonoPaciente("3101234567")
                .franja(franja)
                .sintomas("Dolor de cabeza fuerte desde hace dos dias")
                .estadoTriage(EstadoTriage.PENDIENTE)
                .estado(EstadoCita.AGENDADA)
                .fechaCreacion(LocalDateTime.of(2026, 11, 20, 9, 0))
                .build();
    }
}
