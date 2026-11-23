package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarMedicoUseCase {

    private final MedicoGateway medicoGateway;

    public Medico ejecutar(String codigo) {
        return medicoGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoEncontradoException("MEDICO_NO_ENCONTRADO", "No existe el medico " + codigo));
    }
}
