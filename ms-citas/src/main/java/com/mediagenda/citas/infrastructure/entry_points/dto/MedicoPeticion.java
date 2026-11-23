package com.mediagenda.citas.infrastructure.entry_points.dto;

/** Cuerpo de POST /api/v1/medicos. */
public record MedicoPeticion(String nombre, String especialidad, String correo) {
}
