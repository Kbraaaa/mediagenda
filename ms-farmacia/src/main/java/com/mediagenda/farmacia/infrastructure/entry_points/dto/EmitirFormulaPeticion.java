package com.mediagenda.farmacia.infrastructure.entry_points.dto;

import java.util.List;

/** Cuerpo de POST /api/v1/formulas. */
public record EmitirFormulaPeticion(String codigoCita, List<ItemPeticion> items) {
}
