package com.mediagenda.pacientes.domain.usecase;

import lombok.experimental.UtilityClass;

/** Validacion de textos obligatorios con largo minimo y maximo. */
@UtilityClass
class Textos {

    static void validar(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        int largo = valor.trim().length();
        if (largo < minimo || largo > maximo) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
    }
}
