package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Codigos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * El paciente dice que no recibio un aviso que si salio: se crea una notificacion nueva con el mismo contenido,
 * canal y destinatario, y se envia. La original queda igual, como constancia del primer envio.
 */
@RequiredArgsConstructor
public class ReenviarNotificacionUseCase {

    private final NotificacionGateway notificaciones;
    private final SolicitudEnvioGateway solicitudEnvio;
    private final Despachador despachador;

    public Notificacion ejecutar(String codigo) {
        Notificacion original = new ConsultarNotificacionUseCase(notificaciones).ejecutar(codigo);
        if (original.getEstado() != EstadoNotificacion.ENVIADA && original.getEstado() != EstadoNotificacion.LEIDA) {
            throw new ConflictoException("NOTIFICACION_NO_ENTREGADA", "La notificacion " + codigo + " esta "
                    + original.getEstado() + ": solo se reenvia lo que ya salio (para FALLIDA use el reintento)");
        }
        LocalDateTime ahora = despachador.ahora();
        Notificacion copia = notificaciones.guardar(Notificacion.builder()
                .idNotificacion(UUID.randomUUID())
                .codigo(Codigos.notificacion(ahora.getYear(), notificaciones.siguienteConsecutivo()))
                .tipo(original.getTipo())
                .canal(original.getCanal())
                .codigoPaciente(original.getCodigoPaciente())
                .destinatario(original.getDestinatario())
                .asunto(original.getAsunto())
                .cuerpo(original.getCuerpo())
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(ahora)
                .build());
        solicitudEnvio.solicitar(copia.getCodigo());
        return copia;
    }
}
