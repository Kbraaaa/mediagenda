package com.mediagenda.pacientes.domain.model;

import lombok.experimental.UtilityClass;

/**
 * Convencion de ID legible: prefijo PAC + guion + consecutivo de 6 digitos (PAC-000123).
 * El consecutivo lo entrega una secuencia de PostgreSQL, asi que nunca se repite.
 */
@UtilityClass
public class CodigoPaciente {

    public static final String PREFIJO = "PAC-";

    public static String formatear(long consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser positivo");
        }
        return String.format("%s%06d", PREFIJO, consecutivo);
    }
}
