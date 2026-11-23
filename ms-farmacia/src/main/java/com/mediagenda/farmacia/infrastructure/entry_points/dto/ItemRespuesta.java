package com.mediagenda.farmacia.infrastructure.entry_points.dto;

public record ItemRespuesta(String codigoMedicamento, String medicamento, String presentacion, int cantidad,
                            String indicaciones) {
}
