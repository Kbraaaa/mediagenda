package com.mediagenda.farmacia.domain.model.gateway;

import com.mediagenda.farmacia.domain.model.DatosCita;

import java.util.Optional;

/** Consulta a ms-citas (REST). Lanza ServicioNoDisponibleException si no responde. */
public interface CitaGateway {

    Optional<DatosCita> buscar(String codigoCita);
}
