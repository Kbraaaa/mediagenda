package com.mediagenda.notificaciones.infrastructure.mapper;

import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter.NotificacionEntity;
import org.springframework.stereotype.Component;

@Component
public class EntidadesMapper {

    public Notificacion toDomain(NotificacionEntity e) {
        return Notificacion.builder()
                .idNotificacion(e.getIdNotificacion())
                .codigo(e.getCodigo())
                .eventoId(e.getEventoId())
                .tipo(e.getTipo())
                .canal(e.getCanal())
                .codigoPaciente(e.getCodigoPaciente())
                .destinatario(e.getDestinatario())
                .asunto(e.getAsunto())
                .cuerpo(e.getCuerpo())
                .estado(e.getEstado())
                .intentos(e.getIntentos())
                .ultimoError(e.getUltimoError())
                .fechaCreacion(e.getFechaCreacion())
                .fechaEnvio(e.getFechaEnvio())
                .build();
    }

    public NotificacionEntity toEntity(Notificacion n) {
        NotificacionEntity e = new NotificacionEntity();
        e.setIdNotificacion(n.getIdNotificacion());
        e.setCodigo(n.getCodigo());
        e.setEventoId(n.getEventoId());
        e.setTipo(n.getTipo());
        e.setCanal(n.getCanal());
        e.setCodigoPaciente(n.getCodigoPaciente());
        e.setDestinatario(n.getDestinatario());
        e.setAsunto(n.getAsunto());
        e.setCuerpo(n.getCuerpo());
        e.setEstado(n.getEstado());
        e.setIntentos(n.getIntentos());
        e.setUltimoError(n.getUltimoError());
        e.setFechaCreacion(n.getFechaCreacion());
        e.setFechaEnvio(n.getFechaEnvio());
        return e;
    }
}
