package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.DatosPaciente;

import java.util.Optional;

/** Consulta a ms-pacientes (REST). Lanza ServicioNoDisponibleException si no responde. */
public interface PacienteGateway {

    Optional<DatosPaciente> buscar(String codigoPaciente);
}
