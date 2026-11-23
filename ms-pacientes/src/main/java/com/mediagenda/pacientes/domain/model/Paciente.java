package com.mediagenda.pacientes.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Paciente de la IPS. El id (UUID) es interno; hacia afuera se usa el codigo legible PAC-000123.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    private UUID idPaciente;
    private String codigo;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String tipoSangre;
    private String alergias;
    private boolean activo;
    private LocalDateTime fechaRegistro;
}
