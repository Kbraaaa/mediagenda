package com.mediagenda.pacientes.infrastructure.entry_points.dto;

import java.time.LocalDate;

/** Cuerpo de POST /pacientes y PUT /pacientes/{codigo}. */
public record PacientePeticion(
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        LocalDate fechaNacimiento,
        String tipoSangre,
        String alergias) {
}
