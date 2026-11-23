package com.mediagenda.farmacia.domain.model;

/** Filtros opcionales del listado de formulas (null = sin filtro). */
public record FiltroFormulas(String codigoPaciente, EstadoFormula estado) {
}
