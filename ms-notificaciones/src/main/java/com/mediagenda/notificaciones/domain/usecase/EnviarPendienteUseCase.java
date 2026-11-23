package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import lombok.RequiredArgsConstructor;

/** Lo ejecuta el hilo de envio: toma una notificacion PENDIENTE y la despacha. */
@RequiredArgsConstructor
public class EnviarPendienteUseCase {

    private final NotificacionGateway notificaciones;
    private final Despachador despachador;

    public void ejecutar(String codigo) {
        notificaciones.buscarPorCodigo(codigo)
                .filter(n -> n.getEstado() == EstadoNotificacion.PENDIENTE)
                .ifPresent(despachador::despachar);
    }
}
