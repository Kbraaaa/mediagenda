package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.EventoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.ItemFormula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entrega los medicamentos de una formula. Todo pasa en UNA transaccion:
 * 1) se bloquea la formula y los medicamentos (SELECT ... FOR UPDATE), 2) se revisa el stock real,
 * 3) si alcanza para todo se descuenta y la formula queda DISPENSADA; si falta algo, no se toca nada.
 * Dos dispensaciones simultaneas del mismo medicamento esperan su turno y nunca dejan stock negativo.
 */
@RequiredArgsConstructor
public class DispensarFormulaUseCase {

    private final FormulaGateway formulas;
    private final MedicamentoGateway medicamentos;
    private final TransaccionGateway transaccion;
    private final EventoGateway eventos;
    private final Clock reloj;

    public Formula ejecutar(String codigo) {
        Formula dispensada = transaccion.ejecutar(() -> {
            Formula formula = Validaciones.emitida(formulas.bloquearPorCodigo(codigo), codigo);
            List<UUID> ids = formula.getItems().stream()
                    .map(i -> i.medicamento().getIdMedicamento()).sorted().toList();
            Map<UUID, Medicamento> bloqueados = medicamentos.bloquear(ids).stream()
                    .collect(Collectors.toMap(Medicamento::getIdMedicamento, Function.identity()));

            String faltantes = formula.getItems().stream()
                    .filter(i -> bloqueados.get(i.medicamento().getIdMedicamento()).getStock() < i.cantidad())
                    .map(i -> i.medicamento().getNombre() + " (pide " + i.cantidad() + ", hay "
                            + bloqueados.get(i.medicamento().getIdMedicamento()).getStock() + ")")
                    .collect(Collectors.joining(", "));
            if (!faltantes.isEmpty()) {
                throw new ConflictoException("STOCK_INSUFICIENTE", "No hay unidades suficientes de: " + faltantes);
            }

            for (ItemFormula item : formula.getItems()) {
                Medicamento m = bloqueados.get(item.medicamento().getIdMedicamento());
                medicamentos.actualizarStock(m.getIdMedicamento(), m.getStock() - item.cantidad());
            }
            LocalDateTime ahora = LocalDateTime.now(reloj);
            formulas.actualizarEstado(codigo, EstadoFormula.DISPENSADA, ahora);
            formula.setEstado(EstadoFormula.DISPENSADA);
            formula.setFechaDispensacion(ahora);
            return formula;
        });
        eventos.publicar(EventoFormula.de(EventoFormula.DISPENSADA, dispensada, "Sus medicamentos fueron entregados"));
        return dispensada;
    }
}
