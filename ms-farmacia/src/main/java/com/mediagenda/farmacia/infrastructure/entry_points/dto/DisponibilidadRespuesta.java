package com.mediagenda.farmacia.infrastructure.entry_points.dto;

import java.util.List;

public record DisponibilidadRespuesta(String codigoFormula, boolean dispensable, List<Item> items) {

    public record Item(String codigoMedicamento, String medicamento, int pide, int hay, int faltan) {
    }
}
