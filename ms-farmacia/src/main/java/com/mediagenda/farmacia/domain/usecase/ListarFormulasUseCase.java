package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarFormulasUseCase {

    private final FormulaGateway formulas;

    public Pagina<Formula> ejecutar(FiltroFormulas filtro, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        return formulas.listar(filtro, pagina, tamano);
    }
}
