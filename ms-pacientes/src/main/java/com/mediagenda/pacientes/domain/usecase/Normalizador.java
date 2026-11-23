package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import lombok.experimental.UtilityClass;

import java.util.Locale;

/** Limpia los datos antes de guardar: quita espacios y deja el correo en minusculas. */
@UtilityClass
class Normalizador {

    static Paciente normalizar(Paciente datos) {
        return datos.toBuilder()
                .numeroDocumento(datos.getNumeroDocumento().trim())
                .nombres(datos.getNombres().trim())
                .apellidos(datos.getApellidos().trim())
                .correo(datos.getCorreo().trim().toLowerCase(Locale.ROOT))
                .telefono(vacioANulo(datos.getTelefono()))
                .tipoSangre(vacioANulo(datos.getTipoSangre()))
                .alergias(vacioANulo(datos.getAlergias()))
                .build();
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
