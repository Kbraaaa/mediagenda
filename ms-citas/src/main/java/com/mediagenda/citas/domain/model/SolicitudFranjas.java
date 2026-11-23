package com.mediagenda.citas.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Peticion para abrir agenda: de horaInicio a horaFin, en bloques de duracionMinutos. */
public record SolicitudFranjas(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos) {
}
