package com.mediagenda.notificaciones.infrastructure.entry_points.dto;

import java.time.LocalDateTime;

public record NotificacionRespuesta(String codigo, String tipo, String canal, String codigoPaciente, String destinatario,
                                    String asunto, String cuerpo, String estado, int intentos, String ultimoError,
                                    LocalDateTime fechaCreacion, LocalDateTime fechaEnvio) {
}
