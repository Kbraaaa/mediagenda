package com.mediagenda.farmacia.infrastructure.entry_points.dto;

/** Cuerpo de POST /api/v1/medicamentos. */
public record MedicamentoPeticion(String nombre, String presentacion, Integer stock) {
}
