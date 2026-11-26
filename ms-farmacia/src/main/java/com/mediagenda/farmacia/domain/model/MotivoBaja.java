package com.mediagenda.farmacia.domain.model;

import java.util.Arrays;
import java.util.Locale;

/** Razones por las que unidades de un medicamento salen del inventario sin entregarse a un paciente. */
public enum MotivoBaja {
    VENCIMIENTO,
    DETERIORO,
    RETIRO_SANITARIO;

    public static MotivoBaja desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El motivo de la baja es obligatorio");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Motivo de baja no valido: " + valor + ". Validos: "
                    + Arrays.toString(values()));
        }
    }
}
