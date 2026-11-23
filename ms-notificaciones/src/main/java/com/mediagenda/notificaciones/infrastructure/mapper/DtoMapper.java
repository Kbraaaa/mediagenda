package com.mediagenda.notificaciones.infrastructure.mapper;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Pagina;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.NotificacionRespuesta;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.PaginaRespuesta;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DtoMapper {

    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static EstadoNotificacion estadoOpcional(String valor) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            return null;
        }
        return Arrays.stream(EstadoNotificacion.values())
                .filter(e -> e.name().equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado invalido: use PENDIENTE, ENVIADA, FALLIDA o LEIDA"));
    }

    public NotificacionRespuesta toRespuesta(Notificacion n) {
        return new NotificacionRespuesta(n.getCodigo(), n.getTipo(), n.getCanal().name(), n.getCodigoPaciente(), n.getDestinatario(),
                n.getAsunto(), n.getCuerpo(), n.getEstado().name(), n.getIntentos(), n.getUltimoError(),
                n.getFechaCreacion(), n.getFechaEnvio());
    }

    public PaginaRespuesta<NotificacionRespuesta> toRespuesta(Pagina<Notificacion> p) {
        return new PaginaRespuesta<>(p.contenido().stream().map(this::toRespuesta).toList(),
                p.pagina(), p.tamano(), p.totalElementos(), p.totalPaginas());
    }
}
