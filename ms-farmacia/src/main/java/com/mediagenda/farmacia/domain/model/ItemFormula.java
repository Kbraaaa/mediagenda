package com.mediagenda.farmacia.domain.model;

/** Un renglon de la formula: que medicamento, cuantas unidades y como tomarlo. */
public record ItemFormula(Long idItem, Medicamento medicamento, int cantidad, String indicaciones) {
}
