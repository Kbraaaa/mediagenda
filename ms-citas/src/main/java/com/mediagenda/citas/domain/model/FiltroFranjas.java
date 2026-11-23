package com.mediagenda.citas.domain.model;

import java.time.LocalDate;

/** Filtros del listado de franjas (codigo de medico y fecha opcionales). */
public record FiltroFranjas(String codigoMedico, LocalDate fecha, boolean soloDisponibles) {
}
