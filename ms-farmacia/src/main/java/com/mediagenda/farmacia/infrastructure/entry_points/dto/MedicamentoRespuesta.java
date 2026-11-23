package com.mediagenda.farmacia.infrastructure.entry_points.dto;

public record MedicamentoRespuesta(String codigo, String nombre, String presentacion, int stock, boolean activo) {
}
