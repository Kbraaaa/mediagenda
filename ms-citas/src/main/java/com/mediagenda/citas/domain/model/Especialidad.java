package com.mediagenda.citas.domain.model;

import java.util.Arrays;

/** Especialidades que atiende la IPS. Es la lista cerrada que puede sugerir el triage. */
public enum Especialidad {
    MEDICINA_GENERAL,
    CARDIOLOGIA,
    DERMATOLOGIA,
    PEDIATRIA,
    GINECOLOGIA,
    ORTOPEDIA,
    NEUROLOGIA,
    PSIQUIATRIA,
    OFTALMOLOGIA,
    OTORRINOLARINGOLOGIA,
    GASTROENTEROLOGIA,
    NEUMOLOGIA;

    public static Especialidad desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La especialidad es obligatoria");
        }
        String normalizado = valor.trim().toUpperCase().replace(' ', '_');
        return Arrays.stream(values())
                .filter(e -> e.name().equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Especialidad invalida: " + valor));
    }
}
