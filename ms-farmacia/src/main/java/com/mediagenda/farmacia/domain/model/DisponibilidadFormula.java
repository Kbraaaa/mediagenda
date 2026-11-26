package com.mediagenda.farmacia.domain.model;

import java.util.List;

/** Si hay existencias para entregar una formula emitida, item por item. */
public record DisponibilidadFormula(String codigoFormula, boolean dispensable, List<Item> items) {

    public record Item(String codigoMedicamento, String medicamento, int pide, int hay, int faltan) {
    }
}
