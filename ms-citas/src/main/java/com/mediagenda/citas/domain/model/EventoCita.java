package com.mediagenda.citas.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Evento que se publica (RabbitMQ en local, Amazon SQS en AWS) para que ms-notificaciones envie el aviso.
 * El eventoId es unico: notificaciones lo usa para no enviar dos veces el mismo aviso.
 */
public record EventoCita(
        String eventoId,
        String tipo,
        String codigoCita,
        String codigoPaciente,
        String nombrePaciente,
        String correoPaciente,
        String telefonoPaciente,
        String medico,
        String especialidad,
        LocalDate fecha,
        LocalTime hora,
        String detalle) {

    public static final String AGENDADA = "cita.agendada";
    public static final String REPROGRAMADA = "cita.reprogramada";
    public static final String CANCELADA = "cita.cancelada";
    public static final String RECORDATORIO = "cita.recordatorio";
    public static final String TRIAGE_COMPLETADO = "cita.triage.completado";

    public static EventoCita de(String tipo, Cita cita, String detalle) {
        Franja franja = cita.getFranja();
        return new EventoCita(
                UUID.randomUUID().toString(),
                tipo,
                cita.getCodigo(),
                cita.getCodigoPaciente(),
                cita.getNombrePaciente(),
                cita.getCorreoPaciente(),
                cita.getTelefonoPaciente(),
                franja.getMedico().getNombre(),
                franja.getMedico().getEspecialidad().name(),
                franja.getFecha(),
                franja.getHoraInicio(),
                detalle);
    }
}
