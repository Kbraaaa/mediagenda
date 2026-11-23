package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import lombok.RequiredArgsConstructor;

/** Anula una formula que aun no se ha entregado (por ejemplo, si el medico se equivoco). */
@RequiredArgsConstructor
public class AnularFormulaUseCase {

    private final FormulaGateway formulas;

    public Formula ejecutar(String codigo) {
        Formula formula = Validaciones.emitida(formulas.buscarPorCodigo(codigo), codigo);
        formulas.actualizarEstado(codigo, EstadoFormula.ANULADA, null);
        formula.setEstado(EstadoFormula.ANULADA);
        return formula;
    }
}
