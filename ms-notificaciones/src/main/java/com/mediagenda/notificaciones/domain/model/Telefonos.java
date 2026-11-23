package com.mediagenda.notificaciones.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

/** Convierte el telefono del paciente al formato internacional E.164 que exige el envio de SMS. */
public final class Telefonos {

    private static final Pattern INTERNACIONAL = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern CELULAR_COLOMBIA = Pattern.compile("^3\\d{9}$");
    private static final Pattern CELULAR_COLOMBIA_CON_57 = Pattern.compile("^573\\d{9}$");

    private Telefonos() {
    }

    /** "310 123 4567" -> "+573101234567". Vacio si no es un celular valido. */
    public static Optional<String> aE164(String telefono) {
        if (telefono == null) {
            return Optional.empty();
        }
        String limpio = telefono.replaceAll("[\\s\\-().]", "");
        if (INTERNACIONAL.matcher(limpio).matches()) {
            return Optional.of(limpio);
        }
        if (CELULAR_COLOMBIA.matcher(limpio).matches()) {
            return Optional.of("+57" + limpio);
        }
        if (CELULAR_COLOMBIA_CON_57.matcher(limpio).matches()) {
            return Optional.of("+" + limpio);
        }
        return Optional.empty();
    }
}
