package com.mediagenda.pacientes.domain.model;

import java.util.List;

/** Resultado de revisar una sustancia contra las alergias registradas del paciente. */
public record VerificacionAlergia(String codigoPaciente, String sustancia, boolean alerta, List<String> coincidencias) {
}
