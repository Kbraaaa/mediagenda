package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Aviso;
import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.Codigos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Plantillas;
import com.mediagenda.notificaciones.domain.model.Telefonos;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Convierte un evento en un correo y, si aplica, en un SMS. Es idempotente: si el mismo evento llega
 * dos veces (RabbitMQ y SQS garantizan "al menos una vez"), el segundo se ignora gracias al eventoId.
 * El SMS solo se envia si esta habilitado, el evento lo amerita y el paciente tiene un celular valido.
 */
public class ProcesarEventoUseCase {

    private final NotificacionGateway notificaciones;
    private final Despachador despachador;
    private final boolean smsHabilitado;

    public ProcesarEventoUseCase(NotificacionGateway notificaciones, Despachador despachador, boolean smsHabilitado) {
        this.notificaciones = notificaciones;
        this.despachador = despachador;
        this.smsHabilitado = smsHabilitado;
    }

    /** Solo correo. */
    public ProcesarEventoUseCase(NotificacionGateway notificaciones, Despachador despachador) {
        this(notificaciones, despachador, false);
    }

    /** Devuelve el aviso por correo (el SMS, si lo hay, queda guardado aparte). */
    public Optional<Notificacion> ejecutar(EventoRecibido evento) {
        if (evento.eventoId() == null || evento.eventoId().isBlank()) {
            throw new IllegalArgumentException("El evento no tiene eventoId");
        }
        if (evento.correoPaciente() == null || evento.correoPaciente().isBlank()) {
            throw new IllegalArgumentException("El evento " + evento.eventoId() + " no trae correo del paciente");
        }
        if (notificaciones.existeEvento(evento.eventoId())) {
            return Optional.empty();
        }
        Aviso aviso = Plantillas.redactar(evento);
        Notificacion correo = despachador.despachar(notificaciones.guardar(
                nueva(evento, Canal.CORREO, evento.correoPaciente().trim(), aviso.asunto(), aviso.cuerpo())));
        if (smsHabilitado) {
            Optional<String> telefono = Telefonos.aE164(evento.telefonoPaciente());
            Optional<String> mensaje = Plantillas.redactarSms(evento);
            if (telefono.isPresent() && mensaje.isPresent()) {
                despachador.despachar(notificaciones.guardar(
                        nueva(evento, Canal.SMS, telefono.get(), "SMS: " + aviso.asunto(), mensaje.get())));
            }
        }
        return Optional.of(correo);
    }

    private Notificacion nueva(EventoRecibido evento, Canal canal, String destinatario, String asunto, String cuerpo) {
        LocalDateTime ahora = despachador.ahora();
        return Notificacion.builder()
                .idNotificacion(UUID.randomUUID())
                .codigo(Codigos.notificacion(ahora.getYear(), notificaciones.siguienteConsecutivo()))
                .eventoId(evento.eventoId())
                .tipo(evento.tipo() == null ? "desconocido" : evento.tipo())
                .canal(canal)
                .codigoPaciente(evento.codigoPaciente())
                .destinatario(destinatario)
                .asunto(recortar(asunto, 200))
                .cuerpo(recortar(cuerpo, 2000))
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(ahora)
                .build();
    }

    static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }
}
