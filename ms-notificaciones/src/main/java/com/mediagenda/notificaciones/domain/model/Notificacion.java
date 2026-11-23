package com.mediagenda.notificaciones.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Aviso al paciente. eventoId une el aviso con el evento que lo origino (null si es manual). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    public static final String TIPO_MANUAL = "manual";

    private UUID idNotificacion;
    private String codigo;
    private String eventoId;
    private String tipo;
    @Builder.Default
    private Canal canal = Canal.CORREO;
    private String codigoPaciente;
    private String destinatario;
    private String asunto;
    private String cuerpo;
    private EstadoNotificacion estado;
    private int intentos;
    private String ultimoError;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
}
