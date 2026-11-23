package com.mediagenda.citas.domain.model;

import lombok.experimental.UtilityClass;

/**
 * Convencion de ID legible. El consecutivo sale de una secuencia de PostgreSQL, asi que no se repite.
 * Medico: MED-0001. Cita: CIT-2026-000045 (lleva el anio porque crece todo el tiempo).
 */
@UtilityClass
public class Codigos {

    public static String medico(long consecutivo) {
        validar(consecutivo);
        return String.format("MED-%04d", consecutivo);
    }

    public static String cita(int anio, long consecutivo) {
        validar(consecutivo);
        return String.format("CIT-%d-%06d", anio, consecutivo);
    }

    private static void validar(long consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser positivo");
        }
    }
}
