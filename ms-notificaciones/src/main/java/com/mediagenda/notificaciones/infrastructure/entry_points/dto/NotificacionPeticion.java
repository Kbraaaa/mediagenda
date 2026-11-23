package com.mediagenda.notificaciones.infrastructure.entry_points.dto;

/** Cuerpo de POST /api/v1/notificaciones (aviso manual). */
public record NotificacionPeticion(String destinatario, String asunto, String cuerpo, String codigoPaciente) {
}
