package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.DisponibilidadFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Antes de entregar, revisa si hay existencias suficientes para cada item de una formula emitida. No modifica nada. */
@RequiredArgsConstructor
public class VerificarDisponibilidadUseCase {

    private final FormulaGateway formulas;

    public DisponibilidadFormula ejecutar(String codigo) {
        Formula formula = Validaciones.emitida(formulas.buscarPorCodigo(codigo), codigo);
        List<DisponibilidadFormula.Item> items = formula.getItems().stream()
                .map(i -> new DisponibilidadFormula.Item(i.medicamento().getCodigo(), i.medicamento().getNombre(),
                        i.cantidad(), i.medicamento().getStock(), Math.max(0, i.cantidad() - i.medicamento().getStock())))
                .toList();
        boolean dispensable = items.stream().allMatch(i -> i.faltan() == 0);
        return new DisponibilidadFormula(formula.getCodigo(), dispensable, items);
    }
}
