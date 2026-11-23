package com.mediagenda.farmacia.infrastructure.entry_points.dto;

/** Formato comun de error: {"codigo": "...", "mensaje": "..."}. */
public record ErrorRespuesta(String codigo, String mensaje) {
}
