package com.mediagenda.pacientes.domain.model;

import java.util.Arrays;

/** Tipos de documento de identidad aceptados en Colombia. */
public enum TipoDocumento {
    CC, // cedula de ciudadania
    TI, // tarjeta de identidad
    CE, // cedula de extranjeria
    PA; // pasaporte

    public static TipoDocumento desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(valor.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de documento invalido: use CC, TI, CE o PA"));
    }
}
