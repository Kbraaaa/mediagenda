package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import lombok.RequiredArgsConstructor;

/** Consulta una cita por su codigo (incluye el resultado del triage si ya esta listo). */
@RequiredArgsConstructor
public class ConsultarCitaUseCase {

    private final CitaGateway citaGateway;

    public Cita ejecutar(String codigo) {
        return citaGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoEncontradoException("CITA_NO_ENCONTRADA", "No existe la cita " + codigo));
    }
}
