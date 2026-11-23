package com.mediagenda.citas.infrastructure.entry_points.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Cuerpo de POST /api/v1/medicos/{codigo}/franjas. Ejemplo: 2026-11-24, 08:00 a 12:00, cada 30 min. */
public record AbrirAgendaPeticion(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Integer duracionMinutos) {
}
