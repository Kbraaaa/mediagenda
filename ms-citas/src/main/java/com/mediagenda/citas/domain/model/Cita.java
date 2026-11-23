package com.mediagenda.citas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cita medica. Guarda una copia del nombre, correo y telefono del paciente al momento de agendar,
 * para poder notificar sin consultar a ms-pacientes en cada aviso.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    private UUID idCita;
    private String codigo;
    private String codigoPaciente;
    private String nombrePaciente;
    private String correoPaciente;
    private String telefonoPaciente;
    private Franja franja;
    private String sintomas;
    private Especialidad especialidadSugerida;
    private Prioridad prioridad;
    private String motivoTriage;
    private EstadoTriage estadoTriage;
    private EstadoCita estado;
    private boolean recordatorioEnviado;
    private LocalDateTime fechaCreacion;
}
