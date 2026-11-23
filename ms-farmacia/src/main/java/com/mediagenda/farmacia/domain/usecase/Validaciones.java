package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/** Reglas compartidas por varios casos de uso de farmacia. */
@UtilityClass
public class Validaciones {

    public static void texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        int largo = valor.trim().length();
        if (largo < minimo || largo > maximo) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
    }

    public static void rango(Integer valor, String campo, int minimo, int maximo) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " debe estar entre " + minimo + " y " + maximo);
        }
    }

    /** Exige que la formula exista y siga EMITIDA (no se puede dispensar ni anular dos veces). */
    public static Formula emitida(Optional<Formula> encontrada, String codigo) {
        Formula formula = encontrada.orElseThrow(() ->
                new NoEncontradoException("FORMULA_NO_ENCONTRADA", "No existe la formula " + codigo));
        if (formula.getEstado() != EstadoFormula.EMITIDA) {
            throw new ConflictoException("FORMULA_NO_EMITIDA",
                    "La formula " + codigo + " esta " + formula.getEstado() + " y ya no se puede modificar");
        }
        return formula;
    }
}
