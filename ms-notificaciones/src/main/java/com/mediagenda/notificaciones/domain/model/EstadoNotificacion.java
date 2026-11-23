package com.mediagenda.notificaciones.domain.model;

/** PENDIENTE (por enviar), ENVIADA, FALLIDA (el correo no salio) o LEIDA. */
public enum EstadoNotificacion {
    PENDIENTE, ENVIADA, FALLIDA, LEIDA
}
