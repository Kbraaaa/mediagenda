package com.mediagenda.farmacia.domain.model.gateway;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Pagina;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FormulaGateway {

    Formula guardar(Formula formula);

    Optional<Formula> buscarPorCodigo(String codigo);

    /** Igual que buscarPorCodigo pero bloquea la fila: evita dispensar dos veces la misma formula. */
    Optional<Formula> bloquearPorCodigo(String codigo);

    Pagina<Formula> listar(FiltroFormulas filtro, int pagina, int tamano);

    boolean existeActivaParaCita(String codigoCita);

    void actualizarEstado(String codigo, EstadoFormula estado, LocalDateTime fechaDispensacion);

    long siguienteConsecutivo();
}
