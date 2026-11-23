package com.mediagenda.citas.domain.model;

/** Filtros opcionales del listado de citas (null = sin filtro). */
public record FiltroCitas(String codigoPaciente, String codigoMedico, EstadoCita estado) {
}
