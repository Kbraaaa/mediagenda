package com.mediagenda.pacientes.infrastructure.entry_points.dto;

/** Formato comun de error en todos los microservicios: {"codigo": "...", "mensaje": "..."}. */
public record ErrorRespuesta(String codigo, String mensaje) {
}
