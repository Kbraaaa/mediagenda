package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import lombok.RequiredArgsConstructor;

/** El paciente leyo el aviso. Solo aplica a avisos ENVIADA; marcar dos veces no es error. */
@RequiredArgsConstructor
public class MarcarLeidaUseCase {

    private final NotificacionGateway notificaciones;

    public Notificacion ejecutar(String codigo) {
        Notificacion n = new ConsultarNotificacionUseCase(notificaciones).ejecutar(codigo);
        if (n.getEstado() == EstadoNotificacion.LEIDA) {
            return n;
        }
        if (n.getEstado() != EstadoNotificacion.ENVIADA) {
            throw new ConflictoException("NOTIFICACION_NO_ENVIADA",
                    "La notificacion " + codigo + " esta " + n.getEstado() + " y no se ha entregado");
        }
        n.setEstado(EstadoNotificacion.LEIDA);
        return notificaciones.guardar(n);
    }
}
