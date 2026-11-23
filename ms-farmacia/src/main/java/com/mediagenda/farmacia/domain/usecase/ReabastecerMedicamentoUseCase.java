package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import lombok.RequiredArgsConstructor;

/**
 * Ingreso de unidades al inventario. Es una accion distinta a dispensar (que descuenta):
 * cada una tiene su propio caso de uso por el principio de responsabilidad unica.
 */
@RequiredArgsConstructor
public class ReabastecerMedicamentoUseCase {

    private final MedicamentoGateway medicamentos;

    public Medicamento ejecutar(String codigo, Integer cantidad) {
        Validaciones.rango(cantidad, "cantidad", 1, 10000);
        Medicamento medicamento = medicamentos.buscarPorCodigo(codigo).orElseThrow(() ->
                new NoEncontradoException("MEDICAMENTO_NO_ENCONTRADO", "No existe el medicamento " + codigo));
        medicamentos.sumarStock(medicamento.getIdMedicamento(), cantidad);
        medicamento.setStock(medicamento.getStock() + cantidad);
        return medicamento;
    }
}
