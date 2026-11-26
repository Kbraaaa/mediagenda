package com.mediagenda.farmacia.domain.model;

/** Resultado de dar de baja unidades: el medicamento con su stock nuevo, cuantas salieron y por que. */
public record BajaInventario(Medicamento medicamento, int cantidad, MotivoBaja motivo) {
}
