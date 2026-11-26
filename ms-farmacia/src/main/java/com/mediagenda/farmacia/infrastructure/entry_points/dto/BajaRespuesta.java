package com.mediagenda.farmacia.infrastructure.entry_points.dto;

public record BajaRespuesta(String codigo, String nombre, String presentacion, int cantidadDadaDeBaja,
                            String motivo, int stock) {
}
