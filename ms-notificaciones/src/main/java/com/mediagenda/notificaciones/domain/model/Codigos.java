package com.mediagenda.notificaciones.domain.model;

import lombok.experimental.UtilityClass;

/** Convencion de ID legible: NOT-2026-000310. */
@UtilityClass
public class Codigos {

    public static String notificacion(int anio, long consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser positivo");
        }
        return String.format("NOT-%d-%06d", anio, consecutivo);
    }
}
