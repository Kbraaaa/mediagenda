package com.mediagenda.citas.infrastructure.entry_points.dto;

/** Cuerpo de POST /api/v1/citas. */
public record AgendarCitaPeticion(String codigoPaciente, Long idFranja, String sintomas) {
}
