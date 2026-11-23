package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionEntity {

    @Id
    @Column(name = "id_notificacion")
    private UUID idNotificacion;

    @Column(nullable = false, unique = true, length = 16)
    private String codigo;

    @Column(name = "evento_id", length = 40)
    private String eventoId;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Canal canal;

    @Column(name = "codigo_paciente", length = 20)
    private String codigoPaciente;

    @Column(nullable = false, length = 120)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 2000)
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
