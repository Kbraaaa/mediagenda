package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarFormulaUseCase {

    private final FormulaGateway formulas;

    public Formula ejecutar(String codigo) {
        return formulas.buscarPorCodigo(codigo).orElseThrow(() ->
                new NoEncontradoException("FORMULA_NO_ENCONTRADA", "No existe la formula " + codigo));
    }
}
