package com.mediagenda.farmacia.infrastructure.entry_points.dto;

/** Cuerpo de PATCH /api/v1/medicamentos/{codigo}/stock: unidades que ingresan. */
public record ReabastecerPeticion(Integer cantidad) {
}
