package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Pagina;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarNotificacionesUseCase {

    private final NotificacionGateway notificaciones;

    public Pagina<Notificacion> ejecutar(FiltroNotificaciones filtro, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        return notificaciones.listar(filtro, pagina, tamano);
    }
}
