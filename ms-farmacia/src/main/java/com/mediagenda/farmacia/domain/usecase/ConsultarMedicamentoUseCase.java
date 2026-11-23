package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarMedicamentoUseCase {

    private final MedicamentoGateway medicamentos;

    public Medicamento ejecutar(String codigo) {
        return medicamentos.buscarPorCodigo(codigo).orElseThrow(() ->
                new NoEncontradoException("MEDICAMENTO_NO_ENCONTRADO", "No existe el medicamento " + codigo));
    }
}
