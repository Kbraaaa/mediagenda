package com.mediagenda.notificaciones.domain.model;

/**
 * Evento que llega (RabbitMQ en local, Amazon SQS en AWS) desde ms-citas o ms-farmacia. Reune los campos de ambos:
 * los que no aplican a un tipo de evento llegan en null.
 */
public record EventoRecibido(
        String eventoId,
        String tipo,
        String codigoPaciente,
        String nombrePaciente,
        String correoPaciente,
        String telefonoPaciente,
        String codigoCita,
        String medico,
        String especialidad,
        String fecha,
        String hora,
        String codigoFormula,
        String medicamentos,
        String detalle) {
}
