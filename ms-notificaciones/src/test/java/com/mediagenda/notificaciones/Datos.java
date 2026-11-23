package com.mediagenda.notificaciones;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import com.mediagenda.notificaciones.domain.model.Notificacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public final class Datos {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

    private Datos() {
    }

    public static EventoRecibido eventoCita(String tipo) {
        return new EventoRecibido("evt-1", tipo, "PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567",
                "CIT-2026-000001", "Andrea Lopez", "MEDICINA_GENERAL", "2026-11-21", "08:00:00",
                null, null, "detalle del evento");
    }

    public static EventoRecibido eventoFormula(String tipo) {
        return new EventoRecibido("evt-2", tipo, "PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567",
                "CIT-2026-000001", null, null, null, null, "FOR-2026-000001", "20 x Acetaminofen", "Lista");
    }

    public static Notificacion notificacion(EstadoNotificacion estado) {
        return Notificacion.builder()
                .idNotificacion(UUID.fromString("99999999-0000-0000-0000-000000000001"))
                .codigo("NOT-2026-000001")
                .eventoId("evt-1")
                .tipo("cita.agendada")
                .codigoPaciente("PAC-000001")
                .destinatario("laura@correo.com")
                .asunto("Cita agendada CIT-2026-000001")
                .cuerpo("Hola Laura")
                .estado(estado)
                .intentos(1)
                .fechaCreacion(LocalDateTime.of(2026, 11, 20, 10, 0))
                .build();
    }
}
