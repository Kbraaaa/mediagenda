package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import lombok.RequiredArgsConstructor;

/** Reintento manual de un aviso FALLIDA: vuelve a PENDIENTE y se envia en segundo plano. */
@RequiredArgsConstructor
public class ReintentarNotificacionUseCase {

    private final NotificacionGateway notificaciones;
    private final SolicitudEnvioGateway solicitudEnvio;

    public Notificacion ejecutar(String codigo) {
        Notificacion n = new ConsultarNotificacionUseCase(notificaciones).ejecutar(codigo);
        if (n.getEstado() != EstadoNotificacion.FALLIDA) {
            throw new ConflictoException("NOTIFICACION_NO_FALLIDA",
                    "Solo se reintentan notificaciones FALLIDA; esta esta " + n.getEstado());
        }
        n.setEstado(EstadoNotificacion.PENDIENTE);
        Notificacion guardada = notificaciones.guardar(n);
        solicitudEnvio.solicitar(codigo);
        return guardada;
    }
}
