package com.mediagenda.pacientes;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Datos de prueba reutilizables. */
public final class PacientesDePrueba {

    private PacientesDePrueba() {
    }

    /** Datos validos como llegarian en una peticion (sin id, codigo ni fecha de registro). */
    public static Paciente datosValidos() {
        return Paciente.builder()
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1069123456")
                .nombres("Laura Valentina")
                .apellidos("Gomez Rojas")
                .correo("laura.gomez@correo.com")
                .telefono("3101234567")
                .fechaNacimiento(LocalDate.of(1998, 5, 14))
                .tipoSangre("O+")
                .alergias("Penicilina")
                .build();
    }

    /** Paciente ya guardado en la base. */
    public static Paciente guardado() {
        return datosValidos().toBuilder()
                .idPaciente(UUID.fromString("7b8a3f0e-2c4d-4e5f-9a1b-0c2d3e4f5a6b"))
                .codigo("PAC-000001")
                .activo(true)
                .fechaRegistro(LocalDateTime.of(2026, 11, 17, 10, 30))
                .build();
    }
}
