package com.mediagenda.pacientes.infrastructure.entry_points.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lo que el API devuelve de un paciente. No expone el UUID interno. */
public record PacienteRespuesta(
        String codigo,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        LocalDate fechaNacimiento,
        String tipoSangre,
        String alergias,
        boolean activo,
        LocalDateTime fechaRegistro) {
}
