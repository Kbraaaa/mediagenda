package com.mediagenda.farmacia.domain.model;

/** Item pedido al emitir una formula (llega del API, aun sin validar). */
public record SolicitudItem(String codigoMedicamento, Integer cantidad, String indicaciones) {
}
