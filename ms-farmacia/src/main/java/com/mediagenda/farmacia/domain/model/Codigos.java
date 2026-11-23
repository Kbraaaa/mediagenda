package com.mediagenda.farmacia.domain.model;

import lombok.experimental.UtilityClass;

/** Convencion de ID legible: MDT-0001 para medicamentos y FOR-2026-000012 para formulas. */
@UtilityClass
public class Codigos {

    public static String medicamento(long consecutivo) {
        validar(consecutivo);
        return String.format("MDT-%04d", consecutivo);
    }

    public static String formula(int anio, long consecutivo) {
        validar(consecutivo);
        return String.format("FOR-%d-%06d", anio, consecutivo);
    }

    private static void validar(long consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser positivo");
        }
    }
}
